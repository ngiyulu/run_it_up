package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.stripe.model.PaymentIntent
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunSessionService(): BaseService(){

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var bookingRepository: BookingRepository

    fun getRunSession(runSessionId:String): com.example.runitup.mobile.model.RunSession?{
        val db = runSessionRepository.findById(runSessionId)
        if(!db.isPresent){
            return  null
        }
        val bookings = bookingRepository.findBySessionId(runSessionId)
        val session = db.get()
        session.bookings = bookings.toMutableList()
        return  session
    }
    fun joinSession(stripeId: String, runUser: RunUser, paymentId:String, amount:Double): String?{
        // this means the run session is not confirmed which means we just create a hold charge
        // even if the run already started or confirmed, we still want to create a hold
        //we will have a job that will run later that will charge all the users who didn't
        //get charged when the job was confirmed
        return  createHoldCharge(stripeId, runUser, paymentId, amount)
    }

    fun createHoldCharge(stripeId: String, runUser: RunUser, paymentMethodId:String, amount:Double): String?{
        val paymentIntent: PaymentIntent = paymentService.createCharge(true, amount.convertToCents(), "usd", paymentMethodId, stripeId) ?: return null
        return paymentIntent.id
    }

    fun confirmSession(runSession: com.example.runitup.mobile.model.RunSession):List<com.example.runitup.mobile.model.Payment>{
        val newSession = captureHold(runSession)
        val paymentList = createPayment(newSession)
        return  paymentList
    }
    private fun createPayment(runSession: com.example.runitup.mobile.model.RunSession): List<com.example.runitup.mobile.model.Payment>{
        val list = mutableListOf<com.example.runitup.mobile.model.Payment>()
        runSession.bookings.forEach { booking->
            booking.stripePayment.forEach {
                val payment = com.example.runitup.mobile.model.Payment(
                    ObjectId().toString(),
                    it.stripePaymentId,
                    booking.user.userId.orEmpty(),
                    booking.id.toString(),
                    booking.runSessionId,
                    it.amount,
                    it.amount,
                    emptyMap(),
                    it.paymentStatus,
                    com.example.runitup.mobile.model.PaymentType.FEE
                )
                list.add(payment)
            }
        }
        return  list
    }


    private fun captureHold(runSession: com.example.runitup.mobile.model.RunSession): com.example.runitup.mobile.model.RunSession {
        runSession.bookings.forEach {booking->
            booking.stripePayment.forEach {p->
                val paymentIntentObject = paymentService.captureHold(p.stripePaymentId, p.amount.convertToCents())
                if(paymentIntentObject == null){
                    booking.paymentStatus = PaymentStatus.FAILED
                }
                else{
                    p.stripePaymentId = paymentIntentObject.id.orEmpty()
                    booking.paymentStatus = PaymentStatus.PAID
                }

            }
        }
        return  runSession

    }


}