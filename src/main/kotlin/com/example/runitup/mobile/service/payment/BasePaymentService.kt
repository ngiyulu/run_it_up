package com.example.runitup.mobile.service.payment

import com.example.runitup.mobile.model.AuthStatus
import com.example.runitup.mobile.model.BookingPaymentState
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import java.time.Instant

open class BasePaymentService(
    private val authRepo: PaymentAuthorizationRepository,
    private val bookingStateRepo: BookingPaymentStateRepository
){


    protected fun updateAggregateState(bookingId: String, customerId: String, currency: String) {
        // Keep this in sync with your BookingPricingAdjuster.updateAggregateState
        val auths = authRepo.findByBookingId(bookingId)

        val totalAuthorized = auths
            .filter { it.status == AuthStatus.AUTHORIZED || it.status == AuthStatus.REQUIRES_ACTION }
            .sumOf { it.amountAuthorizedCents }

        val totalCaptured = auths.sumOf { it.amountCapturedCents }
        val totalRefunded = auths.sumOf { it.amountRefundedCents }

        val state = bookingStateRepo.findByBookingId(bookingId)
            ?: BookingPaymentState(
                bookingId = bookingId,
                userId = auths.firstOrNull()?.userId ?: "",
                customerId = customerId,
                currency = currency
            )

        state.totalAuthorizedCents = totalAuthorized
        state.totalCapturedCents = totalCaptured
        state.totalRefundedCents = totalRefunded
        state.refundableRemainingCents = (totalCaptured - totalRefunded).coerceAtLeast(0)
        state.latestUpdatedAt = Instant.now()
        bookingStateRepo.save(state)
    }
}