package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Document(collection = CollectionConstants.BOOKING_COLLECTION)
interface BookingRepository : MongoRepository<Booking, String> {

    // existing
    fun findByRunSessionIdAndStatusIn(runSessionId: String,
                                      status: MutableCollection<BookingStatus>): List<Booking>

    fun findByUserIdAndStatusIn(userId: String,
                                status: MutableCollection<BookingStatus>): List<Booking>

    fun findByUserIdAndRunSessionIdAndStatusIn(userId: String,
                                               runSessionId: String,
                                               status: MutableCollection<BookingStatus>): Booking?

    @Query("{'createdAt': { \$gte: ?0, \$lte: ?1 } }")
    fun findAllByDateBetweenByUser(startInclusive: Date, endInclusive: Date, pageable: Pageable): Page<Booking>

    @Query("{'createdAt': { \$eq: ?0} }")
    fun findByDate(date: Date, pageable: Pageable): Page<Booking>

    // ---- New, commonly-used queries ----

    // Session timelines (paged)
    fun findAllByRunSessionIdOrderByCreatedAtDesc(runSessionId: String, pageable: Pageable): Page<Booking>
    fun findAllByRunSessionIdAndStatusInOrderByCreatedAtDesc(
        runSessionId: String,
        statuses: Collection<BookingStatus>,
        pageable: Pageable
    ): Page<Booking>

    // Counts & existence
    fun countByRunSessionIdAndStatus(runSessionId: String, status: BookingStatus): Long
    fun existsByRunSessionIdAndUserIdAndStatusIn(runSessionId: String, userId: String, statuses: Collection<BookingStatus>): Boolean

    // Payment/state drill-downs
    fun findAllByCustomerId(customerId: String, pageable: Pageable): Page<Booking>
    fun findAllByPaymentMethodId(paymentMethodId: String): List<Booking>
    fun findAllByPaymentStatus(paymentStatus: com.example.runitup.mobile.enum.PaymentStatus, pageable: Pageable): Page<Booking>

    // Waitlist / promotions
    fun findAllByRunSessionIdAndStatus(runSessionId: String, status: BookingStatus, pageable: Pageable): Page<Booking>
    fun findAllByRunSessionIdAndPromotedAtIsNotNullOrderByPromotedAtDesc(runSessionId: String): List<Booking>

    // Locks (ops)
    fun findAllByIsLockedTrueOrderByIsLockedAtAsc(): List<Booking>

    // User timelines
    fun findAllByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<Booking>
    fun findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId: String, start: Date, end: Date, pageable: Pageable): Page<Booking>

    // Quick “latest” per user/session
    fun findFirstByUserIdAndRunSessionIdOrderByCreatedAtDesc(userId: String, runSessionId: String): Booking?
}
