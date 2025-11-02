package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Otp
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.OTP_COLLECTION)
interface OtpRepository : MongoRepository<com.example.runitup.mobile.model.Otp, String> {
    @Query("{userId:'?0'}")
    fun findByUser(userId: String): Otp?

    fun findByPhoneNumberAndIsActive(phoneNumber:String, isActive: Boolean):  Otp?
}