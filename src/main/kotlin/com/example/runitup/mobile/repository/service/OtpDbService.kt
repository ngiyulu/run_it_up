package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.model.Otp
import com.example.runitup.mobile.repository.OtpRepository
import com.example.runitup.mobile.service.NumberGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service


@Service
class OtpDbService {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var numberGenerator: NumberGenerator


    @Autowired
    lateinit var otpRepository: OtpRepository

    fun getOtp(phoneNumber: String): Otp?{
//        val query = Query()
//            .addCriteria(Criteria.where("phoneNumber").`is`(phoneNumber))
//            .addCriteria(Criteria.where("isActive").`is`(true))
//        return mongoTemplate.findOne(query, Otp::class.java)
        return otpRepository.findByPhoneNumberAndIsActive(phoneNumber,true)
    }

    fun generateOtp(userId: String?, phone: String): Otp {
        val q = Query(
            Criteria.where("phoneNumber").`is`(phone)
                .and("isActive").`is`(true)
        )
        val u = Update().set("isActive", false)
        mongoTemplate.updateFirst(q, u, Otp::class.java)

        val code = numberGenerator.generateCode(4)

        return  otpRepository.save(
            Otp(
                code = code,
                phoneNumber = phone,
                userId = userId,
            )
        )
    }

    fun disableOtp(otp: Otp): Otp {
        otp.isActive = false
        return otpRepository.save(otp)
    }

}