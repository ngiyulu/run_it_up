package com.example.runitup.model

import com.example.runitup.dto.payment.CardModel
import com.example.runitup.enum.Sex
import com.example.runitup.enum.SkillLevel
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User(
    @Id var id: String? = ObjectId().toString(),
    var firstName: String,
    var lastName: String,
    var dob: String,
    var verifiedPhone: Boolean = false,
    var email: String,
    var loggedInAt: Long,
    var internalNotes: Long,
    var phoneNumber: String,
    var stripeId: String?,
    var defaultPayment:String?,
    var credit: Double = 0.0,
    var rating: Double? = null,
    var payments: List<CardModel>?,
    var sex: Sex,
    var auth:String = "",
    var runSessions:MutableList<RunSession>,
    var skillLevel: SkillLevel
): BaseModel(){

    fun getFullName(): String{
        return "$firstName ${lastName}"
    }

    fun removeAuth(){
        auth = ""
    }
}