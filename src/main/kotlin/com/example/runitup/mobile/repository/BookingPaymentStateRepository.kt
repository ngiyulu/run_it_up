package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.BookingPaymentState
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.BOOKING_STATE_COLLECTION)
interface BookingPaymentStateRepository : MongoRepository<BookingPaymentState, String> {

    fun findByBookingId(bookingId: String): BookingPaymentState?

    fun save(state: BookingPaymentState): BookingPaymentState

    // 🧾 Retrieve by user
    fun findAllByUserId(userId: String): List<BookingPaymentState>

    // 💳 Retrieve by customer (Stripe-level reconciliation)
    fun findAllByCustomerId(customerId: String): List<BookingPaymentState>

    // 💰 Retrieve by status (for admin dashboards or retry queues)
    fun findAllByStatus(status: String): List<BookingPaymentState>

    // 📆 Retrieve recent updates
    fun findAllByLatestUpdatedAtAfter(since: Instant): List<BookingPaymentState>

    // ⚙️ Combined filters for analytical views
    fun findAllByStatusAndLatestUpdatedAtAfter(status: String, since: Instant): List<BookingPaymentState>

    // 📊 Aggregation-friendly finder for reporting
    @Query("{ 'latestUpdatedAt': { \$gte: ?0, \$lte: ?1 } }")
    fun findAllUpdatedBetween(startInclusive: Instant, endInclusive: Instant): List<BookingPaymentState>
}
