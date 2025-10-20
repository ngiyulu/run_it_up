// src/main/kotlin/com/example/sms/SmsController.kt
package com.example.runitup.mobile.rest.v1.restcontroller.dev

import com.example.runitup.mobile.clicksend.SmsService
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile

@Validated
@RestController
@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles
@RequestMapping("/api/sms")
class SmsRestController(
    private val smsService: SmsService
) {

    data class SendSmsRequest(
        @field:NotBlank val to: String,                 // E.164, e.g. +13125551234
        @field:NotBlank @field:Length(max = 1600) val body: String,
        val tag: String? = null
    )

    @PostMapping("/send")
    fun send(@RequestBody req: SendSmsRequest): ResponseEntity<Map<String, Any>> = runBlocking {
        val accepted = smsService.sendSmsDetailed(req.to, req.body, req.tag)
        ResponseEntity.ok(mapOf("accepted" to accepted))
    }

    @GetMapping("/status/{messageId}")
    fun send(@PathVariable messageId: String): ResponseEntity<Map<String, Any>> = runBlocking {
        val accepted = smsService.fetchStatus(messageId)
        ResponseEntity.ok(mapOf("accepted" to accepted))
    }
}
