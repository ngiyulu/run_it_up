package com.example.runitup.model

import com.example.runitup.enum.Sex
import com.example.runitup.enum.SkillLevel
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
    var verifiedPhone: Boolean = false,
    var email: String = "",
    var loggedInAt: Long = 0,
    var internalNotes: String = "",
    var phoneNumber: String = "",
    var stripeId: String? = null,
    var defaultPayment:String = "",
    var credit: Double = 0.0,
    var rating: Double = 0.0,
    var payments: List<com.example.runitup.web.rest.v1.dto.payment.CardModel>? = null,
    var sex: Sex? = null,
    var auth:String = "",
    var imageUrl: String? = null,
    var runSessions:MutableList<RunSession>? = null,
    var skillLevel: SkillLevel ? = null
): BaseModel(){

    fun getFullName(): String{
        return "$firstName ${lastName}"
    }

    fun removeAuth(){
        auth = ""
    }
}