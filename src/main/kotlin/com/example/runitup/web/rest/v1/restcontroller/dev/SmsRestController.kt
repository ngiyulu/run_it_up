package com.example.runitup.web.rest.v1.restcontroller.dev

import com.example.runitup.service.SmsService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sms")
@Validated
class SmsRestController() {

    @Autowired
    lateinit var sms: SmsService
    @PostMapping("/send")
    fun send(@RequestBody req: SendSmsRequest): SendSmsResponse {
        val sid = sms.sendSms(req.to, req.message)
        val status = if (sid == null) "disabled" else "queued"
        return SendSmsResponse(sid = sid, status = status)
    }

    data class SendSmsRequest(
        // E.164 validation (simple)
        @field:NotBlank
        @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
        val to: String,
        @field:NotBlank
        val message: String
    )

    data class SendSmsResponse(val sid: String?, val status: String)
}