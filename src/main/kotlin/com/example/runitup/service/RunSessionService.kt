package com.example.runitup.service

import com.example.runitup.enum.PaymentStatus
import com.example.runitup.extensions.convertToCents
import com.example.runitup.model.Payment
import com.example.runitup.model.PaymentType
import com.example.runitup.model.RunSession
import com.example.runitup.repository.BookingRepository
import com.example.runitup.repository.RunSessionRepository
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

    fun getRunSession(runSessionId:String): RunSession?{
        val db = runSessionRepository.findById(runSessionId)
        if(!db.isPresent){
            return  null
        }
        val bookings = bookingRepository.findBySessionId(runSessionId)
        val session = db.get()
        session.bookings = bookings.toMutableList()
        return  session
    }
    fun joinSession(stripeId: String, runUser: com.example.runitup.web.rest.v1.dto.RunUser, paymentId:String, amount:Double): String?{
        // this means the run session is not confirmed which means we just create a hold charge
        // even if the run already started or confirmed, we still want to create a hold
        //we will have a job that will run later that will charge all the users who didn't
        //get charged when the job was confirmed
        return  createHoldCharge(stripeId, runUser, paymentId, amount)
    }

    fun createHoldCharge(stripeId: String, runUser: com.example.runitup.web.rest.v1.dto.RunUser, paymentMethodId:String, amount:Double): String?{
        val paymentIntent: PaymentIntent = paymentService.createCharge(true, amount.convertToCents(), "usd", paymentMethodId, stripeId) ?: return null
        return paymentIntent.id
    }

    fun confirmSession(runSession: RunSession):List<Payment>{
        val newSession = captureHold(runSession)
        val paymentList = createPayment(newSession)
        return  paymentList
    }
    private fun createPayment(runSession: RunSession): List<Payment>{
        val list = mutableListOf<Payment>()
        runSession.bookings.forEach { booking->
            booking.stripePayment.forEach {
                val payment = Payment(
                    ObjectId().toString(),
                    it.stripePaymentId,
                    booking.user.userId.orEmpty(),
                    booking.id.toString(),
                    booking.runSessionId,
                    it.amount,
                    it.amount,
                    emptyMap(),
                    it.paymentStatus,
                    PaymentType.FEE
                )
                list.add(payment)
            }
        }
        return  list
    }


    private fun captureHold(runSession: RunSession): RunSession{
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