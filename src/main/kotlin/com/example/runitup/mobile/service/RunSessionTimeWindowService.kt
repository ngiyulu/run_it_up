package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RunSessionTimeWindowService(
    private val runSessionService: RunSessionService,
    private val bookingRepository: BookingRepository

) {
    private val logger = myLogger()


    /**
     * Process up to [maxSessions] PENDING sessions that:
     *  - start within the next [windowMinutes] minutes
     *  - have not yet had the one-hour notification sent
     *
     * Uses atomic findAndModify so multiple workers won't double-notify.
     */
    /**
     * Processes up to [maxSessions] PENDING sessions that:
     *  - start within the next [windowMinutes] minutes
     *  - have not yet had the one-hour notification sent
     *
     * Returns the list of sessions processed.
     */
    fun processOneHourBeforeRunSession(
        nowUtc: Instant = Instant.now(),
        windowMinutes: Long = 60,
        maxSessions: Int = 2
    ): List<RunSession> {

        val processed = mutableListOf<RunSession>()

        repeat(maxSessions) {
            val claimed = runSessionService.claimNextSessionForOneHourNotification(nowUtc, windowMinutes)
            if (claimed == null) {
                // No more matching sessions found
                setBooking(processed)
                return processed
            }

            processed += claimed
        }

        setBooking(processed)
        return processed
    }

    fun setBooking(list:List<RunSession>){
        list.forEach {
            val bookings = getBooking(it.id.orEmpty())
            it.bookings = bookings.toMutableList()
        }
    }


    fun getBooking(sessionId:String): List<Booking>{
        return  bookingRepository.findAllByRunSessionIdAndStatus(sessionId, BookingStatus.JOINED, null).content
    }
}
