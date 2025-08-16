package com.example.runitup.dto.initialize

import com.example.runitup.dto.FirebaseTokenModel
import com.example.runitup.dto.MyLocationModel

class InitializeRequest(
    val userId: String?,
    val firebaseTokenModel: FirebaseTokenModel?,
    val location: MyLocationModel?

)
