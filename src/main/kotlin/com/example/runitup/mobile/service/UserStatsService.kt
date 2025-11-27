package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.UserStat
import org.springframework.stereotype.Service


@Service
class UserStatsService(
    private  val bookingRepository: BookingRepository,
    private val runSessionRepository: RunSessionRepository,
    private val timeService: TimeService
) {

    fun getUserStats(userId:String):UserStat{
        var games = 0
        var facilities = 0
        var duration = 0L
        val list:MutableList<RunSession> = mutableListOf()
        val booking = bookingRepository.findByUserIdAndStatusIn(userId, mutableListOf(BookingStatus.COMPLETED))
        booking.forEach {
            games++
            val visits = runSessionRepository.findByIdentifier(it.runSessionId)
            if(visits != null){
                list.add(visits)
                duration += timeService.minutesBetween(visits.startTime, visits.endTime)

            }

        }
        facilities = countUniqueGymsVisited(list)

        return  UserStat(games, facilities, duration)
    }

    fun countUniqueGymsVisited(visits: List<RunSession>): Int {
        return visits
            .mapNotNull { it.gym?.id?.trim()?.lowercase() }
            .filter { it.isNotBlank() }   // <-- ignore empty strings
            .toSet()
            .size
    }
}