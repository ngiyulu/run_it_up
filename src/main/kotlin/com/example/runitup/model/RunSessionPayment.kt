package com.example.runitup.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

class RunSessionPayment(
    @Id var id: String? = ObjectId().toString(),
    var runSessionId: String,
    val payments:List<Payment>,
    val total: Double,
    val courtFee:Double,
    val remainder: Double
)