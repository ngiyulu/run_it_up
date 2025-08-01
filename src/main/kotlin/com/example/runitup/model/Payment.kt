package com.example.runitup.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

class Payment(
    @Id var id: String = ObjectId().toString(),
    val chargeId: String,
    val userId: String,
    val sessionId:String,
    val amount: Double)