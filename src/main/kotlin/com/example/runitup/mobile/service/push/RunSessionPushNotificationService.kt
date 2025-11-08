package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.service.PhoneDbService
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.service.PushService
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
        pushService.sendToPhones(getAllUsersPhones(runSession, userId), notification)
    }

    fun runSessionCancelled(userId:String, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "Session cancelled",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getAllUsersPhones(runSession, userId), notification)
    }

    fun userJoinedRunSession(userId:String, user:User, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} joined session",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhoneByUser(userId), notification)
    }

    fun runSessionBookingCancelled(userId:String, runSession: RunSession){
        val notification = PushNotification(
            title = runSession.title,
            body = "You have ben removed from the run session",
            data = mapOf(AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL, SessionId to runSession.id.orEmpty())
        )
        pushService.sendToPhones(getPhoneByUser(userId), notification)
    }



    private fun getAllUsersPhones(runSession: RunSession, userId: String): List<Phone>{
        return phoneRepository.findAllByUserIdIn(
            runSession.bookings.map {
                it.userId }.filter {
                it != userId
            })
    }

    private fun getPhoneByUser(userId: String): List<Phone>{
        return phoneRepository.findAllByUserId(userId)

    }
}