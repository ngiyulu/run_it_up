package com.example.runitup.mobile.model

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.rest.v1.dto.RunUser
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
    var gym: com.example.runitup.mobile.model.Gym? = null,
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    var location: GeoJsonPoint?,
    val date: LocalDate,      // local calendar date at the venue
    var startTime: LocalTime,      // local time at the venue
    var endTime: LocalTime,      // local time at the venue
    val zoneId: String,       // IANA zone, e.g. "America/Chicago"
    val startAtUtc: Instant? = null, // optional cache
    var hostedBy: String?,
    var host:AdminUser? = null,
    var allowGuest: Boolean,
    var duration: Int, // in hours
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var amount: Double = 0.0,
    var total: Double = 0.0,
    var maxPlayer: Int,
    var title: String,
    var updateFreeze: Boolean = false,
    // this gets populated when user fetches a runsession/ by default it's empty
    var bookings: MutableList<Booking> = mutableListOf(),
    var waitList: MutableList<RunUser> = mutableListOf(),
    var players:  MutableList<User> = mutableListOf(),
    var maxGuest: Int,
    var isFree: Boolean  = false,
    var isFull: Boolean = false,
    var minimumPlayer: Int  = 10,
    //booking has more data than bookingList
    var bookingList:MutableList<SessionRunBooking> = mutableListOf(),
    var waitListBooking:MutableList<Booking> = mutableListOf(),
    var status: RunStatus = RunStatus.PENDING,
    var buttonStatus: JoinButtonStatus = JoinButtonStatus.JOIN,
    var userStatus: UserButtonStatus = UserButtonStatus.NONE,
    var showUpdatePaymentButton: Boolean = true,
    var guestUpdateAllowed: Boolean = true,
    var leaveSessionUpdateAllowed: Boolean = true,
    // show remove button on the web portal
    var showRemoveButton: Boolean = true
): BaseModel(){


    fun isSessionFree(): Boolean{
        return amount == 0.0
    }
    fun updateStatus(userId: String){
        isFree = isSessionFree()
        isFull = bookings.size == maxPlayer
        buttonStatus = if(status == RunStatus.CANCELLED || status == RunStatus.ONGOING || status == RunStatus.COMPLETED || status == RunStatus.PROCESSED){
            JoinButtonStatus.HIDE
        } else if(isParticiPant(userId)){
            JoinButtonStatus.UPDATE
        } else if(atFullCapacity()){
            if(isWaitlisted(userId) || isParticiPant(userId)) JoinButtonStatus.HIDE
            else {
                JoinButtonStatus.WAITLIST
            }

        } else {
            JoinButtonStatus.JOIN
        }
        val isPendingOrConfirmed = status == RunStatus.PENDING || status == RunStatus.CONFIRMED
        if(isParticiPant(userId)){
            userStatus = UserButtonStatus.PARTICIPANT
        }
        else if(isWaitlisted(userId)){
            userStatus = UserButtonStatus.WAITLISTED
        }
        else {
            userStatus = UserButtonStatus.NONE
        }
        showUpdatePaymentButton = status == RunStatus.PENDING
        guestUpdateAllowed = isPendingOrConfirmed
        leaveSessionUpdateAllowed = isPendingOrConfirmed
    }

    fun isParticiPant(userId: String): Boolean{
        val findUser = bookingList
            .find { it.userId == userId }

        val waitList = waitList
            .find { it.userId == userId }

        return  findUser != null
    }

    fun isWaitlisted(userId: String): Boolean{

        val waitListed = waitList
            .find { it.userId == userId }

        return  waitListed != null
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

    enum class UserButtonStatus{
        WAITLISTED, PARTICIPANT, NONE

    }
    class SessionRunBooking(var bookingId:String, var userId: String, var partySize:Int)
}