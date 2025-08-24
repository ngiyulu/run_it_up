package com.example.runitup.web.rest.v1.dto.initialize

class InitializeRequest(
    val userId: String?,
    val firebaseTokenModel: com.example.runitup.web.rest.v1.dto.FirebaseTokenModel?,
    val location: com.example.runitup.web.rest.v1.dto.MyLocationModel?

)
