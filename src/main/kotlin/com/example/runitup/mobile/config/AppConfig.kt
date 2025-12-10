package com.example.runitup.mobile.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "runitup.config")
class AppConfig {
    var payment: Boolean = false
    var paymentIos: Boolean = false
    var paymentAndroid: Boolean = false
    var waiverUrl:String = ""
    var messaging:Boolean = false
    var baseUrl: String = ""
    var refundUrl:String = ""
    var privacyUrl:String = ""
    var termsAndConditionUrl:String = ""
    var displayDays:Int = 3
    var smsEnabled:Boolean = false
}