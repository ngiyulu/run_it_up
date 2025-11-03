package com.example.runitup.mobile.rest.v1.dto

data class RefundDecision(
    val eligible: Boolean,
    val percent: Int,         // 0..100
    val refundCents: Long,    // computed from chargeCents * percent
    val reason: String
)