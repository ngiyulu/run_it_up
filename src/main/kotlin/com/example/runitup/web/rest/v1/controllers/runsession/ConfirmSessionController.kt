package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.BookingRepository
import com.example.runitup.repository.PaymentRepository
import com.example.runitup.service.PaymentService
import com.example.runitup.service.RunSessionService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
/// confirm session is when we charge the card, this happens 3 hours before the run
class ConfirmSessionController: BaseController<ConfirmSessionModel, RunSession>() {


    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var paymentRepository:PaymentRepository

    @Autowired
    lateinit var bookinRepository: BookingRepository


    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel): RunSession {
        val run =runSessionService.getRunSession(request.sessionId)?: throw ApiRequestException(text("invalid_session_id"))
        if(run.status == RunStatus.CONFIRMED){
            return  run
        }
        if(run.status != RunStatus.PENDING){
            logger.logError("trying to confirm a run session that's not in pedning", run)
            return  run
        }
        run.status = RunStatus.CONFIRMED
        // we captured the charge in stripe
        // we updated the booking list payment status
        // we created the payment list that needs stored in the payment db
        val paymentList  = runSessionService.confirmSession(run)

        paymentList.forEach {
            paymentRepository.save(it)
        }
        // we storing the bookings because we updated the payment status
        run.bookings.forEach {
            bookinRepository.save(it)
        }
        return run
    }



}