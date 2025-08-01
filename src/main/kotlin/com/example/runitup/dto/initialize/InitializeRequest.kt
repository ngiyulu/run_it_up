package com.example.runitup.dto.initialize

import com.example.runitup.dto.TokenModel

class InitializeRequest(
    val userId: String?,
    val tokenModel: TokenModel?,
    val location: Location?

)
class Location(val longitude: Long, val latitude: Long)