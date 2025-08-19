package com.example.runitup.service

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

    fun createCard(stripeCustomerId: String, token: String): PaymentMethod?{

        try {
            val pmParams = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(
                    PaymentMethodCreateParams.Token.builder()
                        .setToken(token) // e.g. "tok_visa" or a real token from client
                        .build()
                )
                .build()
            var pm = PaymentMethod.create(pmParams)
            pm = pm.attach(
                PaymentMethodAttachParams.builder()
                    .setCustomer(stripeCustomerId)
                    .build()
            )
            return  pm
        }catch (ex: Exception){
            logger.logError(TAG, ex)
            return null
        }


//        return  try {
//            val customer = Customer.retrieve(stripeCustomerId)
//            val params = PaymentSourceCollectionCreateParams.builder().setSource(token).build()
//            customer.sources.create(params)
//        }catch (exception: StripeException){
//            logger.logError(TAG, exception)
//            null
//        }

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


    fun listOfCustomerCards(customerId: String): List<PaymentMethod>? {
        return try {
            val params: MutableMap<String, Any> = HashMap()
            params["customer"] = customerId
            params["type"] = "card"
            val pmCollection: PaymentMethodCollection = PaymentMethod.list(params)
             pmCollection.data
        }catch (ex: StripeException){
            logger.logError(TAG, ex)
            null
        }

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