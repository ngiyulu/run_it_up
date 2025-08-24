package com.example.runitup.repository.service

import com.example.runitup.model.Booking
import com.example.runitup.model.BookingPayment
import com.example.runitup.repository.BookingRepository
import com.example.runitup.service.BaseService
import com.mongodb.client.result.DeleteResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
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
        val res: DeleteResult = mongoTemplate.remove(query, Booking::class.java)
        return res.deletedCount > 0
    }

    fun getBooking(userId: String, runSessionId:String): Booking? {
        val query = Query.query(
            Criteria.where("userId").`is`(userId).and("runSessionId").`is`(runSessionId)
        )
        return mongoTemplate.findOne(query, Booking::class.java)
    }

    fun cancelAllBooking(runId: String): Boolean {
        val query = Query.query(
            Criteria.where("runSessionId").`is`(runId)
        )
        val res: DeleteResult = mongoTemplate.remove(query, Booking::class.java)
        return res.deletedCount > 0
    }
}