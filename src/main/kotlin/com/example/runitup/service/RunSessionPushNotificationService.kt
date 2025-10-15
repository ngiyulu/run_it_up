package com.example.runitup.service

import com.example.runitup.constants.AppConstant
import com.example.runitup.constants.AppConstant.SessionId
import com.example.runitup.constants.ScreenConstant
import com.example.runitup.model.Phone
import com.example.runitup.model.RunSession
import com.example.runitup.repository.service.PhoneDbService
import com.example.runitup.web.rest.v1.dto.PushNotification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class RunSessionPushNotificationService {
    @Autowired
    lateinit var phoneRepository: PhoneDbService
    @Autowired
    lateinit var pushService: PushService
    fun runSessionConfirmed(userId: String, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    fun runSessionCancelled(userId:String, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session cancelled",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    fun newSession(userId:String, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    private fun getPhones(runSession: RunSession, userId: String): List<Phone>{
        return phoneRepository.findAllByUserIdIn(
            runSession.bookings.map {
                it.userId }.filter {
                it != userId
            })
    }
}