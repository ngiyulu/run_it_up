package com.example.runitup.mobile.rest.v1.dto.initialize

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.dto.GuideLine
import com.example.runitup.mobile.rest.v1.dto.UserStat
import org.springframework.beans.factory.annotation.Value

class InitializeResponse(
    val gyms: List<com.example.runitup.mobile.model.Gym>,
    var user: User?,
    var token: String,
    var allowGuest: Boolean,
    var maxGuest: Int,
    var waiverUrl: String = "https://google.com",
    var deeplinkUrl:String = "",
    var supportEmail: String = "",
    var allowedPayment: Boolean = false,
    var guideLines: List<GuideLine>,
    var userStats: UserStat? = null,
    @Value("\${refund_url}")
    var refundUrl:String = "",
    var messagingEnabled:Boolean = false,
    var adminUser: AdminUser? = null,
    var privacyUrl:String = "",
    var termsAndConditionUrl:String = "",
    var displayDays:Int,
    var showDeleteButton:Boolean,
    var waiverAppUrl:String = ""
)