package com.example.runitup.model

import org.bson.types.ObjectId

class AppReview(
    var id: String? = ObjectId().toString(),
    val star:Int,
    var userId:String,
    var feedback: String): BaseModel() {

}