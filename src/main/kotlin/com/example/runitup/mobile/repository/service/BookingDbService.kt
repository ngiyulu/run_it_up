package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.service.BaseService
import com.mongodb.client.result.DeleteResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service


@Service
class BookingDbService: BaseService() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun cancelUserBooking(userId: String): Boolean {
        val query = Query.query(
            Criteria.where("userId").`is`(userId)
        )
        val res: DeleteResult = mongoTemplate.remove(query, com.example.runitup.mobile.model.Booking::class.java)
        return res.deletedCount > 0
    }

    fun getBooking(userId: String, runSessionId:String): com.example.runitup.mobile.model.Booking? {
        val query = Query.query(
            Criteria.where("userId").`is`(userId).and("runSessionId").`is`(runSessionId)
        )
        return mongoTemplate.findOne(query, com.example.runitup.mobile.model.Booking::class.java)
    }

    fun cancelAllBooking(runId: String): Boolean {
        val query = Query.query(
            Criteria.where("runSessionId").`is`(runId)
        )
        val res: DeleteResult = mongoTemplate.remove(query, com.example.runitup.mobile.model.Booking::class.java)
        return res.deletedCount > 0
    }
}