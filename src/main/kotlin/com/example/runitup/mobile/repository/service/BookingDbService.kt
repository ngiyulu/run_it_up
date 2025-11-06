package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
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

    @Autowired
    lateinit var authRepo: PaymentAuthorizationRepository

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository


    fun getBooking(userId: String, runSessionId:String): Booking? {
       return bookingRepository.findByUserIdAndRunSessionIdAndStatusIn(userId, runSessionId, mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED))
    }

    fun cancelAllBooking(runId: String): Boolean {
        val query = Query.query(
            Criteria.where("runSessionId").`is`(runId)
        )
        val res: DeleteResult = mongoTemplate.remove(query, Booking::class.java)
        return res.deletedCount > 0
    }

    fun getBookingList(runSessionId:String): List<Booking>{
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