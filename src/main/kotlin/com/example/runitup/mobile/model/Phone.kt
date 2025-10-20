package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.PhoneType
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

class Phone(
    @Id var id: String = ObjectId().toString(),
    var os: String,
    var model: String,
    var token: String,
    var phoneId: String,
    var userId:String,
    var type: PhoneType
): com.example.runitup.mobile.model.BaseModel()