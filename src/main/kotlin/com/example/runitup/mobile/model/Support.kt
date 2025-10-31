package com.example.runitup.mobile.model

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.model.BaseModel
import java.time.LocalDate

class Support(
    val name:String,
    val email:String = "",
    val description:String ="",
    var status:SupportStatus = SupportStatus.PENDING,
    var notes:String = "",
    var resolvedBy:String? = null,
    var admin: AdminUser? = null,
    var resolvedAt: LocalDate? = null): BaseModel()

enum class SupportStatus{
    PENDING, RESOLVED
}