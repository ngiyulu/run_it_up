package com.example.runitup.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestParam


@Service
class FirebaseService: BaseService(){
    lateinit var phoneService: PhoneService
    fun notifyToken(
        @RequestParam token: String,
        @RequestParam title: String,
        @RequestParam body: String
    ): ResponseEntity<String> = try {
        val msgId = sendToToken(token, title, body)
        ResponseEntity.ok("Sent message ID: $msgId")
    } catch (e: Exception) {
        ResponseEntity
            .status(500)
            .body("Error sending FCM message: ${e.message}")
    }

    @Throws(Exception::class)
    fun sendToToken(token: String, title: String, body: String): String {
        val message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build()
        return FirebaseMessaging.getInstance().send(message)
    }
}