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
    fun updateHoldCharge(chargeId: String, newAmount: Long): Charge? {
        return try {
            val resource = Charge.retrieve(chargeId)
            val params = ChargeUpdateParams.builder().putMetadata("shipping", "express").build()
            return resource.update(params)
        }catch (ex: StripeException){
            logger.logError("update hold charge failed", ex.toString())
              null
        }
    }

    fun createHoldChargeWithToken(token: String, amount: Long): Charge? {
       return try {
            val params = ChargeCreateParams.builder()
                .setAmount(amount) // e.g. 2000 = $20.00
                .setCurrency("usd")
                .setCapture(false) // FALSE means authorization only (hold)
                .setSource(token) // Token from frontend
                .build()
            return Charge.create(params)
        }catch (exception: Exception){
            logger.logError(TAG, exception)
            null
        }

    }

    fun completePayment(chargeId: String?): Charge? {
        return try {
            val charge = Charge.retrieve(chargeId)
            charge.capture()
        }catch (exception: Exception){
            logger.logError(TAG, exception)
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
        } catch (ex: Exception) {
            logger.logError(TAG, ex)
            null
        }
    }

    fun deleteCard( cardId: String): PaymentMethod?{
        return  try {
            val resource = PaymentMethod.retrieve(cardId)
            val params = PaymentMethodDetachParams.builder().build()
            resource.detach(params)
        }catch (exception: StripeException){
            logger.logError(TAG, exception)
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
        }catch (ex: Exception){
            logger.logError(TAG, ex)
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
        }catch (ex: StripeException){
            logger.logError(TAG, ex)
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


    fun refundOrCancel(chargeId: String?): Refund? {
        return try {
            val params = RefundCreateParams.builder()
                .setCharge(chargeId)
                .build()
            return Refund.create(params)

        }catch (exception: Exception){
            logger.logError(TAG, exception)
            null
        }

    }
}