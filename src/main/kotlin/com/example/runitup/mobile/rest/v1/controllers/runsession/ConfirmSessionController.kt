package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.common.repo.PaymentRepository
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
/// confirm session is when we charge the card, this happens 3 hours before the run
class ConfirmSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Autowired
    lateinit var bookingRepository: BookingRepository

    override fun execute(request: ConfirmSessionModel): com.example.runitup.mobile.model.RunSession {
        val run =runSessionService.getRunSession(request.sessionId)?: throw ApiRequestException(text("invalid_session_id"))
        if(run.status == RunStatus.CONFIRMED){
            return run
        }
        if(run.status != RunStatus.PENDING){
            logger.logError("trying to confirm a run session that's not in pending", run)
            return run
        }
        if(!request.overrideMinimum && run.bookings.size < run.minimumPlayer){
            throw ApiRequestException(text("min_player"))
        }
        run.status = RunStatus.CONFIRMED
        if(!run.isSessionFree()){
            // we captured the charge in stripe
            // we updated the booking list payment status
            // we created the payment list that needs stored in the payment db
            val paymentList  = runSessionService.confirmSession(run)

            paymentList.forEach {
                paymentRepository.save(it)
            }
        }
        // we storing the bookings because we updated the payment status
        run.bookings.forEach {
            bookingRepository.save(it)
        }
        return runSessionRepository.save(run)
    }

}