package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.ConfirmSessionModel
import com.example.runitup.enum.PaymentStatus
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Payment
import com.example.runitup.model.RunSession
import com.example.runitup.model.RunSessionPayment
import com.example.runitup.repository.RunSessionPaymentRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ConfirmSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionPaymentRepository: RunSessionPaymentRepository
    override fun execute(request: ConfirmSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
        run.status = RunStatus.CONFIRMED
        var runSessionPayment = createPayment(run, completePayment(run))
        runSessionPayment = runSessionPaymentRepository.save(runSessionPayment)
        run.payment = runSessionPayment
        run = runSessionRepository.save(run)
        return run
    }


    private fun createPayment(runSession: RunSession, payments:List<Payment>): RunSessionPayment{
        val total = payments.sumOf { it.amount }
        val remainder = total - runSession.courtFee
        return RunSessionPayment(payments = payments,
            courtFee = runSession.courtFee,
            remainder = remainder,
            runSessionId = runSession.id.toString(),
            total = total,
        )
    }
    private fun completePayment(runSession: RunSession): List<Payment>{
        val paymentList:MutableList<Payment> = mutableListOf()
        runSession.getPlayersList().forEach { user->
            val charge = paymentService.completePayment(user.stripeChargeId)
            charge?.let { ch ->
                paymentList.add(
                    Payment(
                        chargeId =  ch.id,
                        userId = user.userId.orEmpty(),
                        sessionId = runSession.id.toString(),
                        amount = runSession.amount
                    )
                )
                user.paymentStatus = PaymentStatus.PAID
            } ?:run {
                user.paymentStatus = PaymentStatus.FAILED
            }

        }
        return paymentList

    }
}