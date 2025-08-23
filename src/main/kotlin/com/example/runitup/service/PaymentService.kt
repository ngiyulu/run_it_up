package com.example.runitup.service

import com.example.runitup.dto.payment.CardModel
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.model.User
import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.*
import com.stripe.param.*
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.HashMap


@Service
class PaymentService: BaseService() {

    @Value("\${stripe.api.key}")
    private val stripeApiKey: String? = null

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
    ): PaymentIntent? {
        var captureMethod = PaymentIntentCreateParams.CaptureMethod.MANUAL
        if(!isHold){
            captureMethod = PaymentIntentCreateParams.CaptureMethod.AUTOMATIC
        }
        return try {
            val createParams = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setCaptureMethod(captureMethod) // **key**
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                .setConfirm(true) // confirm immediately (requires a PM id)
                .setPaymentMethod(paymentMethodId)
                // If you want to save the card for later:
                //.setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                .apply {
                    if (customerId != null) setCustomer(customerId)
                }
                .build()

            // Recommended: pass an idempotency key for resilience
            val requestOptions = com.stripe.net.RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build()

            // May return requires_action if 3DS is needed — handle on the client if so.
            PaymentIntent.create(createParams, requestOptions)
        }catch (exception: Exception){
            logger.logError("createHold", exception)
            null
        }
    }



    /**
     * Capture the previously authorized PaymentIntent (charges the customer).
     * You can optionally capture a partial amount <= original authorization.
     */
    fun captureHold(
        paymentIntentId: String,
        captureAmountCents: Long
    ): PaymentIntent? {
       return try {
           val paramsBuilder = PaymentIntentCaptureParams.builder()
           paramsBuilder.setAmountToCapture(captureAmountCents)
           PaymentIntent.retrieve(paymentIntentId).capture(paramsBuilder.build())
       }catch (exception: Exception){
           logger.logError("captureHold", exception)
           null
       }
    }


    /**
     * Cancel the authorization (releases the hold).
     */
    fun cancelHold(paymentIntentId: String, reason: PaymentIntentCancelParams.CancellationReason? = null): PaymentIntent? {
        return try {
            val cancelParams = PaymentIntentCancelParams.builder().apply {
                if (reason != null) setCancellationReason(reason)
            }.build()
            PaymentIntent.retrieve(paymentIntentId).cancel(cancelParams)
        }catch (exception: Exception){
            logger.logError("cancelHold", exception)
            null
        }
    }


    fun createPaymentMethod(stripeCustomerId: String, paymentMethodId: String): PaymentMethod? {
        return try {
            val pm = PaymentMethod.retrieve(paymentMethodId)
            pm.attach(
                PaymentMethodAttachParams.builder()
                    .setCustomer(stripeCustomerId)
                    .build()
            )
        } catch (exception: Exception) {
            logger.logError("createPaymentMethod", exception)
            null
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