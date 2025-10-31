package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.common.service.IdempotencyKeyGenerator
import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.toIntentState
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.IntentState
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.PaymentIntentStateRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.JoinWaitListResponse
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.rest.v1.dto.session.JoinWaitListModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.WaitListPaymentService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinWaitListController: BaseController<JoinWaitListModel, JoinWaitListResponse>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var idempotencyKeyGenerator: IdempotencyKeyGenerator

    @Autowired
    lateinit var paymentService: WaitListPaymentService


    @Autowired
    lateinit var intentStateRepository: PaymentIntentStateRepository



    override fun execute(request: JoinWaitListModel): JoinWaitListResponse {
        val runDb = runSessionRepository.findById(request.sessionId)
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runDb.get()
        // this mean the event is full
        if( user.stripeId == null){
            throw ApiRequestException(text("payment_error"))
        }
        if( !run.isJoinable()){
            throw  ApiRequestException(text("join_error"))
        }
        val idempotencyKey = idempotencyKeyGenerator.generateIdempotencyKey(
            "waitList",
            user.id.orEmpty(), run.id.orEmpty()
        )
        var intentState: IntentState? = null
        //user can only join the waitlist if the run is at full capacity
        if( run.atFullCapacity()){
            if(!run.isFree()){
                val result = paymentService.ensureOffSessionReadyServerSide(user.stripeId.orEmpty(), request.sessionId, idempotencyKey)
                intentState = result.toIntentState(user.id.orEmpty(), run.id.toString(), idempotencyKey)
                intentState = intentStateRepository.save(intentState)
            }
            val runUser = RunUser(
                user.firstName,
                user.lastName,
                user.skillLevel,
                user.id.orEmpty(),
                user.imageUrl,
                0,
                guest = 0
            )
            run.waitList.add(
              runUser
            )
            run.updateTotal()
            val updatedRun = runSessionRepository.save(run)
            bookingRepository.save(
                com.example.runitup.mobile.model.Booking(
                    ObjectId().toString(),
                    1,
                    user.id.orEmpty(),
                    runUser,
                    request.sessionId,
                    emptyList(),
                    PaymentStatus.PENDING,
                    run.amount,
                    run.amount,
                    0,
                    joinedAtFromWaitList = null,
                    intentState = intentState,
                    status = BookingStatus.WAITLISTED
                )

            )
            return JoinWaitListResponse(true, updatedRun)
        }

        // this means the user tried to join the waitlist
        return  JoinWaitListResponse(false, run)

    }
}