package com.example.runitup.service

import com.example.runitup.twilio.TwilioProperties
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URI

@Service
class SmsService(): SmsSender {


    @Autowired
    lateinit var  props: TwilioProperties

    private val log = LoggerFactory.getLogger(javaClass)
    override fun sendSms(toE164: String, body: String): String? {
        if (!props.enabled) return null

        val to = PhoneNumber(toE164)

        val creator = when {
            !props.messagingServiceSid.isNullOrBlank() ->
                // picks the (PhoneNumber, String, String) overload
                Message.creator(to, props.messagingServiceSid, body)

            !props.fromNumber.isNullOrBlank() ->
                // picks the (PhoneNumber, PhoneNumber, String) overload
                Message.creator(to, PhoneNumber(props.fromNumber), body)

            else -> error("Twilio is enabled but neither messagingServiceSid nor fromNumber is set.")
        }
        return creator.create().sid
    }

    override fun sendMms(toE164: String, body: String, mediaUrl: String): String? {
        if (!props.enabled) return null

        val to = PhoneNumber(toE164)
        val media = listOf(URI(mediaUrl))

        val creator = when {
            !props.messagingServiceSid.isNullOrBlank() ->
                Message.creator(to, props.messagingServiceSid, body).setMediaUrl(media)

            !props.fromNumber.isNullOrBlank() ->
                Message.creator(to, PhoneNumber(props.fromNumber), body).setMediaUrl(media)

            else -> error("Twilio is enabled but neither messagingServiceSid nor fromNumber is set.")
        }

        return creator.create().sid
    }

    override fun sendBulk(toList: List<String>, body: String): List<Pair<String, String?>> =
        toList.map { to -> to to sendSms(to, body) }

}

interface SmsSender {
    fun sendSms(toE164: String, body: String): String?         // returns SID or null if disabled
    fun sendMms(toE164: String, body: String, mediaUrl: String): String?
    fun sendBulk(toList: List<String>, body: String): List<Pair<String, String?>> // to -> sid/null
}