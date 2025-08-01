package com.example.runitup.repository.service

import com.example.runitup.dto.RunUser
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.service.TimeAndDateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunSessionRepositoryService {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var timeAndDateService: TimeAndDateService

    fun setPlayerSignedUpListToCancelled(userId: String, session:RunSession): RunSession{
        val hourDiff = timeAndDateService.hoursUntil(session)
        val cancelSessionModel = RunUser.CancelModel(timeAndDateService.getTimeStamp(), RunUser.CancelRefundType.NO_REFUND)
        // full refund
        if(hourDiff >= 5){
            cancelSessionModel.cancelRefundType = RunUser.CancelRefundType.REFUND
        }
        // between 3 and 5 credit
        else if(hourDiff >3){
            cancelSessionModel.cancelRefundType = RunUser.CancelRefundType.CREDIT
        }
        else // no refund

        session.getPlayersList().forEach {
            if(it.userId == userId || it.host == userId){
                it.cancelModel = cancelSessionModel
                it.status = RunUser.RunUserStatus.CANCELLED
            }
        }
        session.updatePlayersFromWaitList()
        return session.let { runSessionRepository.save(it) }
    }
}