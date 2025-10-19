package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.repository.OtpRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors


@Service
class OtpDbService {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate


    @Autowired
    lateinit var otpRepository: OtpRepository

    fun getOtp(phoneNumber: String): com.example.runitup.mobile.model.Otp?{
        val query = Query()
            .addCriteria(Criteria.where("phoneNumber").`is`(phoneNumber))
            .addCriteria(Criteria.where("isActive").`is`(true))
        return mongoTemplate.findOne(query, com.example.runitup.mobile.model.Otp::class.java)
    }

    fun generateOtp(userId: String?, phone: String): com.example.runitup.mobile.model.Otp {
        val q = Query(Criteria.where("phoneNumber").`is`(phone))
        val u = Update().set("isActive", false)
        mongoTemplate.updateFirst(q, u, com.example.runitup.mobile.model.Otp::class.java)

        val code: String = ThreadLocalRandom.current()
            .ints(4, 0, 10)
            .mapToObj(java.lang.String::valueOf)
            .collect(Collectors.joining())

        return  otpRepository.save(
            com.example.runitup.mobile.model.Otp(
                code = code,
                phoneNumber = phone,
                userId = userId,
                created = Date.from(Instant.now())
            )
        )
    }

    fun disableOtp(otp: com.example.runitup.mobile.model.Otp): com.example.runitup.mobile.model.Otp {
        otp.isActive = false
        return otpRepository.save(otp)
    }

}