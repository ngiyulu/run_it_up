package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.repository.service.PhoneDbService
import com.example.runitup.mobile.service.PushService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PaymentPushNotificationService {
    @Autowired
    lateinit var phoneRepository: PhoneDbService

    @Autowired
    lateinit var pushService: PushService

    fun notifyPaymentActionRequired(userId:String, aymentIntentId:String){

    }
}