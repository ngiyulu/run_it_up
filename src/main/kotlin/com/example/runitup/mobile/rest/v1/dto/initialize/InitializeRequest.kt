package com.example.runitup.mobile.rest.v1.dto.initialize

import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import com.example.runitup.mobile.rest.v1.dto.MyLocationModel

class InitializeRequest(
    val userId: String?,
    val tokenModel: FirebaseTokenModel?,
    val location: MyLocationModel?,
    var os: String = "",
    var zoneId:String? = null

)
