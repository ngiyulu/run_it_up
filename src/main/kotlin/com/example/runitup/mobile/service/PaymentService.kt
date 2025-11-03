package com.example.runitup.mobile.service

import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.extensions.mapToUserPayment
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.example.runitup.mobile.rest.v1.dto.payment.CreateChargeResult
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIRequest
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIResponse
import com.stripe.Stripe
import com.stripe.exception.CardException
import com.stripe.exception.StripeException
import com.stripe.model.*
import com.stripe.net.RequestOptions
import com.stripe.param.*
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*


@Service
class PaymentService: BaseService() {

    @Value("\${stripe.api.key}")
    private val stripeApiKey: String? = null

    @Autowired
    lateinit var waitListPaymentService: WaitListPaymentService

    @PostConstruct
    fun init() {
        // for stripe-java < 20.x
        Stripe.apiKey = stripeApiKey

        // if you’re on stripe-java 20.x+, use:
        // StripeConfiguration.setApiKey(stripeApiKey);
    }

    fun createCustomer(user: User):String?{
        return try {
            val params = CustomerCreateParams.builder()
                .setName(user.getFullName())
                .setEmail(user.email)
                .build()
            val customer = Customer.create(params)
            customer.id
        }catch (ex: StripeException){
            print(ex)
            logger.logError(TAG, ex)
            null
        }
    }

    // Method to update the hold charge
    fun updatePaymentIntentAmount(paymentIntentId: String, newAmountCents: Long): PaymentIntent? {
        return try {
            val params = PaymentIntentUpdateParams.builder()
                .setAmount(newAmountCents)
                .build()

            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            paymentIntent.update(params)
        }catch (exception: Exception){
            logger.logError("updatePaymentIntentAmount", exception)
            null
        }
    }


    fun createCharge(
        isHold: Boolean,
        amountCents: Long,
        currency: String,
        paymentMethodId: String,
        customerId: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): CreateChargeResult {
        val captureMethod = if (isHold)
            PaymentIntentCreateParams.CaptureMethod.MANUAL
        else
            PaymentIntentCreateParams.CaptureMethod.AUTOMATIC

        return try {
            val auto = PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                // if your stripe-java doesn't expose AllowRedirects, use putExtraParam below
                .setAllowRedirects(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                )
                .build()

            val builder = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setCaptureMethod(captureMethod)          // MANUAL for holds, AUTOMATIC for capture now
                // .setConfirmationMethod(...)  <-- REMOVE THIS LINE
                .setConfirm(true)                         // ok to keep: server-side confirm
                .setPaymentMethod(paymentMethodId)
                .setAutomaticPaymentMethods(auto)         // keep APM with redirects disabled

            if (customerId != null) {
                builder.setCustomer(customerId)
                // If you plan to reuse this payment method off-session later:
                // builder.setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
            }

            val createParams = builder.build()

            val requestOptions = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build()

            val pi = PaymentIntent.create(createParams, requestOptions)

            val requiresAction = pi.status == "requires_action" || pi.status == "requires_source_action"
            val nextActionType = pi.nextAction?.type
            val redirectUrl = pi.nextAction?.redirectToUrl?.url

            // If Stripe already attached a lastPaymentError on the PI (e.g., authentication_required),
            // surface that too so you can decide whether to prompt for a new card or retry.
            val lastErr = pi.lastPaymentError

            // Treat requires_action as a non-error (UI should handle 3DS).
            // Treat requires_payment_method as an error (card failed / needs replacement).
            val isTerminalOk = pi.status in listOf("succeeded", "requires_capture", "processing")
            val ok = isTerminalOk || requiresAction

            CreateChargeResult(
                ok = ok,
                paymentIntentId = pi.id,
                clientSecret = pi.clientSecret,           // send to client if you handle 3DS client-side
                status = pi.status,
                isHold = isHold,
                amountCents = pi.amount,
                currency = pi.currency,
                customerId = pi.customer,
                paymentMethodId = paymentMethodId,
                idempotencyKey = idempotencyKey,
                requiresAction = requiresAction,
                nextActionType = nextActionType,
                nextActionRedirectUrl = redirectUrl,
                lastPaymentErrorCode = lastErr?.code,
                lastPaymentErrorDeclineCode = lastErr?.declineCode,
                lastPaymentErrorMessage = lastErr?.message,
                // If it's a recoverable failure (e.g., network/temporary) Stripe won't set lastPaymentError;
                // If the failure is due to the card (requires_payment_method), prompt for a new PM instead.
                canRetry = when {
                    pi.status == "processing" -> true
                    pi.status == "requires_payment_method" -> false
                    else -> true
                },
                paymentIntent = pi
            )
        } catch (e: CardException) {
            // Card or SCA-specific issue thrown as exception (less common at PI.create time but possible)
            val se = e.stripeError
            logger.logError("createCharge(CardException)", e)
            CreateChargeResult(
                ok = false,
                status = null,
                isHold = isHold,
                amountCents = amountCents,
                currency = currency.lowercase(),
                customerId = customerId,
                paymentMethodId = paymentMethodId,
                idempotencyKey = idempotencyKey,
                errorType = se?.type ?: "card_error",
                errorCode = e.code,
                declineCode = se?.declineCode,
                message = se?.message ?: e.message,
                canRetry = when (se?.declineCode) {
                    // Some declines may succeed with a retry or another attempt later,
                    // but generally prompt user for another card on hard declines.
                    "insufficient_funds", "do_not_honor", "transaction_not_allowed" -> true
                    else -> false
                }
            )
        } catch (e: StripeException) {
            // API/connection/state errors
            logger.logError("createCharge(StripeException)", e)
            CreateChargeResult(
                ok = false,
                status = null,
                isHold = isHold,
                amountCents = amountCents,
                currency = currency.lowercase(),
                customerId = customerId,
                paymentMethodId = paymentMethodId,
                idempotencyKey = idempotencyKey,
                errorType = e.stripeError?.type ?: "api_error",
                errorCode = e.code,
                declineCode = e.stripeError?.declineCode,
                message = e.message,
                canRetry = true // safe to retry with same idempotency key
            )
        } catch (e: Exception) {
            // Unexpected bug in our code
            logger.logError("createCharge(Exception)", e)
            CreateChargeResult(
                ok = false,
                status = null,
                isHold = isHold,
                amountCents = amountCents,
                currency = currency.lowercase(),
                customerId = customerId,
                paymentMethodId = paymentMethodId,
                idempotencyKey = idempotencyKey,
                errorType = "internal_error",
                message = e.message,
                canRetry = false
            )
        }
    }



    /**
     * Capture the previously authorized PaymentIntent (charges the customer).
     * - captureAmountCents must be <= authorized amount.
     * - Returns a structured result with error details on failure.
     */
    fun captureHold(
        paymentIntentId: String,
        captureAmountCents: Long,
        idempotencyKey: String? = null
    ): CaptureHoldResult {
        return try {
            val params = PaymentIntentCaptureParams.builder()
                .setAmountToCapture(captureAmountCents)
                .build()

            val opts = idempotencyKey?.let {
                RequestOptions.builder().setIdempotencyKey(it).build()
            }

            val pi = if (opts != null) {
                PaymentIntent.retrieve(paymentIntentId).capture(params, opts)
            } else {
                PaymentIntent.retrieve(paymentIntentId).capture(params)
            }

            CaptureHoldResult(
                ok = true,
                paymentIntentId = pi.id,
                status = pi.status,
                amountAuthorizedCents = pi.amount,
                amountCapturableCents = pi.amountCapturable,
                amountCapturedCents = pi.amountReceived,
                currency = pi.currency,
                latestChargeId = pi.latestCharge
            )
        } catch (e: CardException) {
            // Typical card/issuer errors
            val se = e.stripeError
            // Try to fetch current PI snapshot so you can display updated amounts/status
            val current = runCatching { PaymentIntent.retrieve(paymentIntentId) }.getOrNull()
            CaptureHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = current?.status,
                amountAuthorizedCents = current?.amount,
                amountCapturableCents = current?.amountCapturable,
                amountCapturedCents = current?.amountReceived,
                currency = current?.currency,
                latestChargeId = current?.latestCharge,
                errorType = se?.type,
                errorCode = e.code,                 // e.g. "authentication_required", "capture_expired"
                declineCode = se?.declineCode,      // e.g. "insufficient_funds"
                message = se?.message ?: e.message,
                canRetry = when (se?.declineCode) {
                    "insufficient_funds", "transaction_not_allowed", "do_not_honor" -> true
                    else -> false
                }
            )
        } catch (e: StripeException) {
            // API/connection errors, invalid state, etc.
            val current = runCatching { PaymentIntent.retrieve(paymentIntentId) }.getOrNull()
            CaptureHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = current?.status,
                amountAuthorizedCents = current?.amount,
                amountCapturableCents = current?.amountCapturable,
                amountCapturedCents = current?.amountReceived,
                currency = current?.currency,
                latestChargeId = current?.latestCharge,
                errorType = e.stripeError?.type ?: "api_error",
                errorCode = e.code,
                declineCode = e.stripeError?.declineCode,
                message = e.message,
                canRetry = true // transient errors often safe to retry with same idempotency key
            )
        } catch (e: Exception) {
            // Unexpected bug in our code path
            CaptureHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = null,
                message = "Unexpected error: ${e.message}",
                canRetry = false
            )
        }
    }

   // create
    fun createSetupIntentForWaitList(sessionId:String, customerId: String, userId: String, savedPaymentMethodId: String): SetupIntent? {
        return try{
            val params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .setPaymentMethod(savedPaymentMethodId)
                .putMetadata("sessionId", sessionId)
                .putMetadata("userId", userId)
                .setConfirm(true)      // confirm server-side
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .build()
            SetupIntent.create(params)
        }catch (exception: Exception){
            logger.logError("captureHold", exception)
            null
        }
    }


    /**
     * Cancel the authorization (releases the hold).
     */
    /**
     * Cancel (void) a previously authorized PaymentIntent.
     * This releases the hold and prevents future capture.
     */
    fun cancelHold(
        paymentIntentId: String,
        reason: PaymentIntentCancelParams.CancellationReason? = null
    ): CancelHoldResult {
        return try {
            val cancelParams = PaymentIntentCancelParams.builder().apply {
                if (reason != null) setCancellationReason(reason)
            }.build()

            val canceledPI = PaymentIntent.retrieve(paymentIntentId).cancel(cancelParams)

            CancelHoldResult(
                ok = true,
                paymentIntentId = canceledPI.id,
                status = canceledPI.status,
                cancellationReason = canceledPI.cancellationReason,
                amountAuthorizedCents = canceledPI.amount,
                currency = canceledPI.currency,
                message = "PaymentIntent ${canceledPI.id} canceled successfully"
            )
        } catch (e: CardException) {
            val se = e.stripeError
            CancelHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = null,
                errorType = se?.type ?: "card_error",
                errorCode = e.code,
                declineCode = se?.declineCode,
                message = se?.message ?: e.message,
                canRetry = false // cancels usually not retriable
            )
        } catch (e: StripeException) {
            CancelHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = null,
                errorType = e.stripeError?.type ?: "api_error",
                errorCode = e.code,
                declineCode = e.stripeError?.declineCode,
                message = e.message,
                canRetry = true // can retry if network issue
            )
        } catch (e: Exception) {
            CancelHoldResult(
                ok = false,
                paymentIntentId = paymentIntentId,
                status = null,
                message = "Unexpected error: ${e.message}",
                canRetry = false
            )
        }
    }

    fun createPaymentMethod(
        stripeCustomerId: String,
        paymentMethodId: String
    ): PaymentMethodResult {
        return try {
            // Retrieve the PaymentMethod
            val pm = PaymentMethod.retrieve(paymentMethodId)

            // Attach to customer
            val attached = pm.attach(
                PaymentMethodAttachParams.builder()
                    .setCustomer(stripeCustomerId)
                    .build()
            )

            PaymentMethodResult(success = true, paymentMethod = attached)

        } catch (e: StripeException) {
           // logger.error("Stripe API error during attach: ${e.message}", e)
            PaymentMethodResult(
                success = false,
                error = StripeError(
                    code = e.code,
                    message = e.userMessage ?: e.message,
                    type = e.stripeError?.type
                )
            )

        } catch (e: Exception) {
            //logger.error("Unexpected error during attach: ${e.message}", e)
            PaymentMethodResult(
                success = false,
                error = StripeError(message = e.localizedMessage ?: "Unexpected error")
            )
        }
    }

    fun deleteCard( cardId: String): PaymentMethod?{
        return  try {
            val resource = PaymentMethod.retrieve(cardId)
            val params = PaymentMethodDetachParams.builder().build()
            resource.detach(params)
        }catch (exception: StripeException){
            logger.logError("deleteCard", exception)
            null
        }
    }

    fun makeDefaultCard(customerId: String, paymentMethodId: String): Customer? {
        return  try {
            val pm = PaymentMethod.retrieve(paymentMethodId)
            if (pm.customer == null || pm.customer != customerId) {
                val attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build()
                pm.attach(attachParams) // (Best practice is to save via SetupIntent client-side)
            }

            // 2) Set as customer's default for invoices/subscriptions
            val customer = Customer.retrieve(customerId)
            val update = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build()
                ).build()
            customer.update(update)
        }catch (exception: Exception){
            logger.logError("makeDefaultCard", exception)
           null
        }
    }

    fun createPaymentIntent(req: CreatePIRequest): CreatePIResponse? {
        return try {
            require(req.amount > 0) { "amount must be > 0 (in minor units)" }
            require(req.currency.isNotBlank()) { "currency is required" }

            val automaticPMs = PaymentIntentCreateParams.AutomaticPaymentMethods
                .builder().setEnabled(true).build()

            val paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(req.amount)
                .setCurrency(req.currency.lowercase())
                .setAutomaticPaymentMethods(automaticPMs)

            req.customerId?.let { paramsBuilder.setCustomer(it) }
            req.description?.let { paramsBuilder.setDescription(it) }
            req.metadata?.let { paramsBuilder.putAllMetadata(it) }
            paramsBuilder.setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
            val params = paramsBuilder.build()

            // Set an explicit idempotency key when creating the PI
            val requestOptions = req.idempotencyKey?.let {
                RequestOptions.builder().setIdempotencyKey(it).build()
            }

            val pi: PaymentIntent = if (requestOptions != null) {
                PaymentIntent.create(params, requestOptions)
            } else {
                PaymentIntent.create(params)
            }

            // Return the client secret for the client (iOS) to confirm via PaymentSheet/Apple Pay
            CreatePIResponse(
                paymentIntentId = pi.id,
                clientSecret = pi.clientSecret,
                amount = pi.amount,
                currency = pi.currency
            )
        }catch (exception: Exception){
            logger.logError("createPaymentIntent", exception)
            null
        }
    }


    fun listOfCustomerCards(customerId: String): List<CardModel>? {
        return try {
            val params: MutableMap<String, Any> = HashMap()
            params["customer"] = customerId
            params["type"] = "card"
            val pmCollection: PaymentMethodCollection = PaymentMethod.list(params)
            val defaultPayment = getDefaultPayment(customerId)
            print("default payment = $defaultPayment")
            return pmCollection.data.map {
                it.mapToUserPayment(it.id == defaultPayment)
            }
        }catch (exception: StripeException){
            logger.logError("listOfCustomerCards", exception)
            null
        }

    }

    fun createWaitListPayment( customerId: String,
                               paymentMethodId: String,
                               idempotencyKey: String?){
        waitListPaymentService.ensureOffSessionReadyServerSide(customerId, paymentMethodId, idempotencyKey)
    }

    private fun getDefaultPayment(customerId: String): String?{
        // 2) Get the customer's default payment method id (expanded for convenience)
        val custParams = CustomerRetrieveParams.builder()
            .addExpand("invoice_settings.default_payment_method")
            .build()
        val customer = Customer.retrieve(customerId, custParams, null)

        // In stripe-java, default_payment_method is expandable; get the id if present
        return customer.invoiceSettings?.defaultPaymentMethod
    }


}

data class CaptureHoldResult(
    val ok: Boolean,
    val paymentIntentId: String? = null,
    val status: String? = null,                // e.g. "succeeded", "requires_capture", "canceled"
    val amountAuthorizedCents: Long? = null,   // PI.amount
    val amountCapturableCents: Long? = null,   // PI.amountCapturable
    val amountCapturedCents: Long? = null,     // PI.amountReceived
    val currency: String? = null,
    val latestChargeId: String? = null,

    // Error details (when ok=false)
    val errorType: String? = null,             // stripeError.type (card_error, api_error, ...)
    val errorCode: String? = null,             // e.code (authentication_required, capture_expired, ...)
    val declineCode: String? = null,           // stripeError.declineCode (insufficient_funds, do_not_honor, ...)
    val message: String? = null,               // human-readable message
    val canRetry: Boolean = false              // hint for your flow
)


data class CancelHoldResult(
    val ok: Boolean,
    val paymentIntentId: String? = null,
    val status: String? = null,                    // e.g. "canceled"
    val cancellationReason: String? = null,        // e.g. "requested_by_customer"
    val amountAuthorizedCents: Long? = null,
    val currency: String? = null,

    // Error details (when ok=false)
    val errorType: String? = null,                 // stripeError.type (api_error, card_error)
    val errorCode: String? = null,
    val declineCode: String? = null,
    val message: String? = null,
    val canRetry: Boolean = false
)

data class PaymentMethodResult(
    val success: Boolean,
    val paymentMethod: PaymentMethod? = null,
    val error: StripeError? = null
)

data class StripeError(
    val code: String? = null,
    val message: String? = null,
    val type: String? = null
)
