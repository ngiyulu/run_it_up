package com.example.runitup.repository.service

import com.example.runitup.model.Otp
import com.example.runitup.repository.OtpRepository
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
    lateinit var mongo: MongoTemplate

    @Autowired
    lateinit var otpRepository: OtpRepository

    fun getOtp(userId: String): Otp?{
        val query = Query()
            .addCriteria(Criteria.where("userId").`is`(userId))
            .addCriteria(Criteria.where("isActive").`is`(true))
        return mongoTemplate.findOne(query, Otp::class.java)
    }

    fun generateOtp(phone: String): Otp {
        val q = Query(Criteria.where("phoneNumber").`is`(phone))
        val u = Update().set("isActive", false)
        mongo.updateFirst(q, u, Otp::class.java)

        val code: String = ThreadLocalRandom.current()
            .ints(4, 0, 10)
            .mapToObj(java.lang.String::valueOf)
            .collect(Collectors.joining())

        return  otpRepository.save(Otp(code = code, phoneNumber = phone, created =  Date.from(Instant.now())))
    }

    fun disableOtp(otp: Otp): Otp{
        otp.isActive = false
        return otpRepository.save(otp)
    }

}