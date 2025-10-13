package com.example.runitup.model

import com.example.runitup.enum.RunStatus
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Document
data class RunSession(
    @Id var id: String? = ObjectId().toString(),
    var gym:Gym? = null,
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    var location: GeoJsonPoint?,
    val date: LocalDate,      // local calendar date at the venue
    var startTime: LocalTime,      // local time at the venue
    var endTime: LocalTime,      // local time at the venue
    val zoneId: String,       // IANA zone, e.g. "America/Chicago"
    val startAtUtc: Instant? = null, // optional cache
    var hostedBy: String?,
    var allowGuest: Boolean,
    var duration: Int, // in hours
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var amount: Double = 0.0,
    var total: Double = 0.0,
    var maxPlayer: Int,
    var title: String,
    var bookings: MutableList<Booking> = mutableListOf(),
    var waitList: MutableList<com.example.runitup.web.rest.v1.dto.RunUser> = mutableListOf(),
    var players:  MutableList<User>? = null,
    var courtFee: Double = 0.0,
    var maxGuest: Int,
    var bookingList:MutableList<SessionRunBooking> = mutableListOf(),
    var waitListBooking:MutableList<Booking> = mutableListOf(),
    var status: RunStatus = RunStatus.PENDING,
    var buttonStatus: JoinButtonStatus = JoinButtonStatus.JOIN,
    var showUpdatePaymentButton: Boolean = true,
    var guestUpdateAllowed: Boolean = true,
    var leaveSessionUpdateAllowed: Boolean = true
): BaseModel(){


    fun isFree(): Boolean{
        return courtFee == 0.0
    }
    fun updateStatus(userId: String){
        buttonStatus = if(isParticiPant(userId)){
            JoinButtonStatus.UPDATE
        } else if(atFullCapacity()){
            JoinButtonStatus.WAITLIST
        } else if(status == RunStatus.CANCELLED || status == RunStatus.COMPLETED || status == RunStatus.PROCESSED){
            JoinButtonStatus.HIDE
        } else {
            JoinButtonStatus.JOIN
        }
        val isPendingOrConfirmed = status == RunStatus.PENDING || status == RunStatus.CONFIRMED
        showUpdatePaymentButton = status == RunStatus.PENDING
        guestUpdateAllowed = isPendingOrConfirmed
        leaveSessionUpdateAllowed = isPendingOrConfirmed
    }

    fun isParticiPant(userId: String): Boolean{
        val findUser = bookingList
            .find { it.userId == userId }

        return  findUser != null
    }

    fun isDeletable(): Boolean{
        return status == RunStatus.PENDING
    }

    fun isUpdatable(): Boolean{
        return false
    }

    fun isJoinable(): Boolean{
        return status == RunStatus.PENDING ||
                status == RunStatus.CONFIRMED ||
                status == RunStatus.ONGOING
    }
    fun getConversationTitle(): String{
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH) // e.g. "Oct 13"
        val timeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH) // e.g. "4:00PM"

        val formattedDate = date.format(dateFormatter)
        val formattedTime = startTime.format(timeFormatter)
        return "$formattedDate, $formattedTime"
    }

    fun atFullCapacity(): Boolean{
        return bookingSize() >= maxPlayer
    }


    fun userHasBookingAlready(userId: String): Boolean{
        return bookingList.find {
            it.userId ==userId
        } != null
    }



    private fun bookingSize(): Int{
        return bookingList.sumOf { it.partySize }
    }
    fun availableSpots(): Int{
        return  maxPlayer - bookingSize()
    }

    fun updateTotal(){
        total =bookingList.sumOf {
            it.partySize * amount
        }
    }

    enum class JoinButtonStatus{
        JOIN, WAITLIST, UPDATE, HIDE
    }
    class SessionRunBooking(var bookingId:String, var userId: String, var partySize:Int)
}