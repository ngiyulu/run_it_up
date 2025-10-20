package com.example.runitup.mobile.service

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.repository.service.PhoneDbService
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class RunSessionPushNotificationService {
    @Autowired
    lateinit var phoneRepository: PhoneDbService
    @Autowired
    lateinit var pushService: PushService
    fun runSessionConfirmed(userId: String, runSession: com.example.runitup.mobile.model.RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    fun runSessionCancelled(userId:String, runSession: com.example.runitup.mobile.model.RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session cancelled",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    fun newSession(userId:String, runSession: com.example.runitup.mobile.model.RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhones(runSession, userId), notification)
    }

    private fun getPhones(runSession: com.example.runitup.mobile.model.RunSession, userId: String): List<com.example.runitup.mobile.model.Phone>{
        return phoneRepository.findAllByUserIdIn(
            runSession.bookings.map {
                it.userId }.filter {
                it != userId
            })
    }
}