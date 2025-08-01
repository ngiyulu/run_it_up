package com.example.runitup.service.push

import com.example.runitup.service.BaseService
import com.example.runitup.service.FirebaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PushNotificationService: BaseService() {

    @Autowired
    lateinit var firebaseService: FirebaseService



}