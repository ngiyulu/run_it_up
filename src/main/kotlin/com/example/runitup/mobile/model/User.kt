package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.SkillLevel
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ngiyulu.runitup.messaging.runitupmessaging.model.user.Sex
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    @Id var id: String? = ObjectId().toString(),
    var firstName: String = "",
    var lastName: String = "",
    var dob: String =  "",
    var waiverSigned: Boolean = false,
    var waiverUrl:String? = null,
    var waiverAuthorized:Boolean = false,
    var firstSession: Boolean = false,
    var waiverAuthorizedAt: Long = 0,
    var waiverAuthorizedBy: String? = null,
    var verifiedPhone: Boolean = false,
    var email: String = "",
    var loggedInAt: Long = 0,
    var internalNotes: String = "",
    var phoneNumber: String = "",
    var stripeId: String? = null,
    var defaultPayment:String = "",
    var credit: Double = 0.0,
    var rating: Double = 0.0,
    var payments: List<CardModel>? = null,
    var sex: Sex? = null,
    var auth:String = "",
    var imageUrl: String? = null,
    var runSessions:MutableList<RunSession>? = null,
    var skillLevel: SkillLevel? = null,
    var creator: Creator = Creator.USER
): BaseModel(){

    fun getFullName(): String{
        return "$firstName ${lastName}"
    }

    fun approveWaiver(adminId:String, timeStamp:Long, waiverImageUrl:String?, isAuthorized:Boolean){
        waiverAuthorized = isAuthorized
        waiverSigned = true
        waiverAuthorizedBy = adminId
        waiverAuthorizedAt = timeStamp
        waiverImageUrl?.let {
            waiverUrl = it
        }
    }
}

enum class Creator{
    ADMIN, USER
}