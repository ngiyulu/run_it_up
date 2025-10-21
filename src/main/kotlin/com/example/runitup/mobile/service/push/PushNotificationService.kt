package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.service.FirebaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PushNotificationService: com.example.runitup.mobile.service.BaseService() {

    @Autowired
    lateinit var firebaseService: FirebaseService



}