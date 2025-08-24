package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.JoinSessionModel
import com.example.runitup.enum.PaymentStatus
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.convertToCents
import com.example.runitup.model.*
import com.example.runitup.repository.BookingRepository
import com.example.runitup.repository.RefundRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.service.RunSessionService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.abs

@Service
class UpdateSessionGuest: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var refundRepository: RefundRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private  lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionService: RunSessionService

    override fun execute(request: JoinSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        var user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run =runSessionService.getRunSession(request.sessionId)?: throw ApiRequestException(text("invalid_session_id"))
        if(run.atFullCapacity()){
            //TODO: later we can find a way to add the guest to the waitlist
            throw ApiRequestException(text("full_capacity"))
        }
        if( !run.isJoinable()){
            throw  ApiRequestException(text("join_error"))
        }
        // we find the user making the update
        val signedUpUser = run.bookingList.find { it.userId == user.id }
        if(signedUpUser == null){
            logger.logError("error updating session, user is null",  null)
            throw ApiRequestException(text("invalid params"))
        }
         val booking: Booking = run.bookings.find { it.user.userId == user.id.orEmpty() }
             ?: throw ApiRequestException(text("invalid_params"))

        var adding = false
        // this means the user is adding more people
        if(booking.partySize -1 < request.guest){
            adding = true

            // we have to check to make sure the run is not at full capacity
            if(run.atFullCapacity()){
                throw  ApiRequestException(text("full_capacity"))
            }
        }
        val newRequestAmount = (request.getTotalParticipants() * run.amount)
        //we get the differnce between the last request they made and the new request
        // if it's an addition if means we create a new request
        // if it's subtraction it means we either have to descrease the hold amount
        // or we have to do a refund
        val diffAmount = abs(newRequestAmount - booking.stripePayment.last().amount)
        if(adding){
            // we are creating a new hold charge
            logger.logInfo("update session guest", "adding")
            val paymentId = runSessionService.createHoldCharge(user.stripeId.orEmpty(), booking.user, request.paymentMethodId, diffAmount)
                ?: throw ApiRequestException(text("stripe_error"))
            val list = booking.stripePayment.toMutableList().apply {
                add( BookingPayment(newRequestAmount, paymentId))
            }
            booking.stripePayment = list
        }
        else{
            if(run.status == RunStatus.PENDING) {
                // we have to decrease the hold amount
                logger.logInfo("decreasing the hold amount", "decrease amount")
                val stripePayment = booking.stripePayment.last()
                stripePayment.amount = newRequestAmount
                paymentService.updatePaymentIntentAmount(stripePayment.stripePaymentId, newRequestAmount.convertToCents())
            }
            else{
                logger.logInfo("decreasing the hold amount", "refund")
                val refund  = Refund(ObjectId().toString(),
                    userId = user.id.orEmpty(),
                    runId = run.id.orEmpty(), amount =  newRequestAmount,
                    status =  RefundStatus.PENDING,
                    requestedAt = LocalDate.now())
                refundRepository.save(refund)
            }
        }
        booking.partySize = request.guest + 1
        bookingRepository.save(booking)
        return  runSessionRepository.save(run)




    }
}