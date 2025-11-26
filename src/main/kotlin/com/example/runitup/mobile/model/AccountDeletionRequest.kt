package com.example.runitup.mobile.model

import org.bson.types.ObjectId

class AccountDeletionRequest(
    var id: String? = ObjectId().toString(),
    var userId:String
): BaseModel()