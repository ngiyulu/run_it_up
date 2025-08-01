package com.example.runitup.dto

import com.example.runitup.enum.PaymentStatus
import com.example.runitup.model.RunSession
import com.example.runitup.utility.AppUtil
import java.time.LocalDateTime

class RunUser(
    val name: String,
    val userId: String?,
    var stripeChargeId: String?,
    var guest: Int = 0,
    var isGuestUser: Boolean,
    var host: String?,
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    var checkIn: Boolean = false,
    // this is the model
    var cancelModel: CancelModel? = null,
    var status: RunUserStatus = RunUserStatus.GOING,
    var verificationCode: String = AppUtil.generate4DigitCode().toString()

){

    class CancelModel(val cancelAt: Long, var  cancelRefundType: CancelRefundType)

    enum class RunUserStatus{
        CANCELLED, GOING, PLAYED
    }
    enum class CancelRefundType{
        REFUND, CREDIT, NO_REFUND
    }
}