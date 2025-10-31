package com.example.runitup.mobile.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "runitup.config")
class PaymentConfig {
    var payment: Boolean = false
    var paymentIos: Boolean = false
    var paymentAndroid: Boolean = false
}