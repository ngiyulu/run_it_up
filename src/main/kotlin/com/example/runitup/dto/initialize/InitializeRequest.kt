package com.example.runitup.dto.initialize

import com.example.runitup.dto.FirebaseTokenModel

class InitializeRequest(
    val userId: String?,
    val firebaseTokenModel: FirebaseTokenModel?,
    val location: Location?

)
class Location(val longitude: Long, val latitude: Long)