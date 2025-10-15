package com.example.runitup.web.rest.v1.dto.initialize

import com.example.runitup.web.rest.v1.dto.MyLocationModel

class InitializeRequest(
    val userId: String?,
    val tokenModel: com.example.runitup.web.rest.v1.dto.FirebaseTokenModel?,
    val location: MyLocationModel?,
    var os: String = ""

)
