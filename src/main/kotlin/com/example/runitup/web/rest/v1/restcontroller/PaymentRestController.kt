package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.common.repo.PaymentRepository
import com.example.runitup.mobile.model.Payment
import com.example.runitup.mobile.rest.v1.dto.GetPaymentSecretModel
import com.example.runitup.mobile.service.WaitListPaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/admin/payments")
class PaymentController(private val repo: PaymentRepository) {

    @Autowired
    lateinit var waitListPaymentService: WaitListPaymentService
    // by RunSession
    @GetMapping("/by-session/{sessionId}")
    fun bySession(@PathVariable sessionId: String): List<Payment> =
        repo.findAllBySessionId(sessionId)

    // by user
    @GetMapping("/by-user/{userId}")
    fun byUser(@PathVariable userId: String): List<Payment> =
        repo.findAllByUserId(userId)

    @GetMapping("/payment/secret/{intentId}")
    fun getPaymentIntent(@PathVariable intentId: String): GetPaymentSecretModel {
        val secret = waitListPaymentService.getPaymentIntentClientSecret(intentId = intentId)
        return  GetPaymentSecretModel(secret)
    }

    // by date range (local date -> UTC instants)
    @GetMapping("/by-range")
    fun byDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): List<Payment> {
        val startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC)
        val endInstant = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return repo.findAllByCreatedAtBetween(startInstant, endInstant)
    }
}
