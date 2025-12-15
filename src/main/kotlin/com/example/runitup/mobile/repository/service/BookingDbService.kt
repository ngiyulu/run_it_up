package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.service.BaseService
import com.mongodb.client.result.UpdateResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class BookingDbService: BaseService() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var authRepo: PaymentAuthorizationRepository

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository


    fun getBooking(userId: String, runSessionId:String): Booking? {
       return bookingRepository.findByUserIdAndRunSessionIdAndStatusIn(userId, runSessionId, mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED))
    }

    fun cancelAllBooking(runId: String): UpdateResult {
        val query = Query.query(
            Criteria.where("runSessionId").`is`(runId)
        )
        val u = Update()
            .set("status", BookingStatus.CANCELLED)
            .set("cancelledAt", Instant.now())
        return  mongoTemplate.updateMulti(query,  u,  Booking::class.java)
    }

    fun completeAllBooking(runId: String): UpdateResult {
        val query = Query.query(
            Criteria.where("runSessionId").`is`(runId)
                .and("status").`in`(
                    BookingStatus.JOINED,
                    BookingStatus.WAITLISTED
                )
        )

        val update = Update()
            .set("status", BookingStatus.COMPLETED)
            .set("completedAt", Instant.now())

        return mongoTemplate.updateMulti(query, update, Booking::class.java)
    }
    fun tryLock(bookingId: String, now: Instant): Int {
        val query = Query(
            Criteria.where("_id").`is`(bookingId)
                .andOperator(
                    Criteria().orOperator(
                        Criteria.where("isLocked").`is`(false),
                        Criteria.where("isLocked").exists(false)
                    )
                )
        )
        val update = Update()
            .set("isLocked", true)
            .set("isLockedAt", now)

        val result = mongoTemplate.updateFirst(query, update, Booking::class.java)
        return result.modifiedCount.toInt() // 1 if lock succeeded, 0 if already locked
    }

    fun getJoinedBookingList(runSessionId:String): List<Booking>{
        return bookingRepository.findByRunSessionIdAndStatusIn(
            runSessionId,
            mutableListOf(BookingStatus.JOINED)
        )
    }
    fun getBookingDetails(booking: Booking): Booking{
        booking.bookingPaymentState = bookingPaymentStateRepository.findByBookingId(booking.id.orEmpty())
        val active = authRepo.findByBookingId(booking.id.orEmpty())
        booking.paymentAuthorization = active

        return  booking
    }
}