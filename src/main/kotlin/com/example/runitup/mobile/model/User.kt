package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.SkillLevel
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.example.runitup.mobile.service.UserActionRequired
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ngiyulu.runitup.messaging.runitupmessaging.model.user.Sex
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

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
    var waiver:Waiver? = null,

    var firstSession: Boolean = false,
    var waiverAuthorizedAt: Instant? = null,
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
    var creator: Creator = Creator.USER,
    var coordinate: GeoJsonPoint? = null,
    var actions:List<UserActionRequired> = mutableListOf(),
    var linkedAdmin: String? = null,
    var userType: UserType? = null,
    var updatedActivatedStatusAt:Instant? = null,
    var updatedActivatedStatusBy:String? = null,
    var rejectionNotes:String? = null
): BaseModel(){

    fun getFullName(): String{
        return "$firstName $lastName"
    }

    fun approveWaiver(adminId:String, waiverImageUrl:String?, isAuthorized:Boolean){
        waiverAuthorized = isAuthorized
        waiverSigned = true
        waiverAuthorizedBy = adminId
        waiverAuthorizedAt = Instant.now()
        waiverImageUrl?.let {
            waiverUrl = it
        }
    }
}

enum class Creator{
    ADMIN, USER
}

enum class UserType{
    ADMIN, SUPER_ADMIN
}

data class Coordinate(val longitude: Double, val latitude: Double)