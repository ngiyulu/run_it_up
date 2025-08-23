package com.example.runitup.model

import com.example.runitup.enum.PaymentStatus
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
    val metadata: Map<String, String> = emptyMap(),
    val paymentStatus: PaymentStatus,
    val type: PaymentType
): BaseModel()

enum class PaymentType{
    FEE,
    NO_SHOW
}