package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.PaymentStatus
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

class Payment(
    @Id var id: String? = ObjectId().toString(),
    val paymentIntentId: String?,
    val userId: String,
    var bookingId: String,
    val sessionId:String,
    // the amount at the beginning
    val initialAmount: Double,
    // the initial amount when the payment is created
    val amount: Double,
    val paymentStatus: PaymentStatus,
    val type: PaymentType
): com.example.runitup.mobile.model.BaseModel()

enum class PaymentType{
    FEE,
    NO_SHOW
}