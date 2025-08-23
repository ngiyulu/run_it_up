package com.example.runitup.model

import com.example.runitup.dto.RunUser
import com.example.runitup.enum.RunStatus
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.*

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
    var duration: Int, // in hours
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var amount: Double = 0.0,
    var total: Double = 0.0,
    var maxPlayer: Int,
    var title: String,
    var bookings: MutableList<Booking> = mutableListOf(),
    var waitList: MutableList<RunUser> = mutableListOf(),
    var players:  MutableList<User>? = null,
    var courtFee: Double = 0.0,
    var maxGuest: Int,
    var bookingList:MutableList<SessionRunBooking> = mutableListOf(),
    var waitListBooking:MutableList<Booking> = mutableListOf(),
    var status: RunStatus = RunStatus.PENDING,
    var buttonStatus: JoinButtonStatus = JoinButtonStatus.JOIN
): BaseModel(){


    fun updateButtonStatus(userId: String){
        buttonStatus = if(isParticiPant(userId)){
            JoinButtonStatus.UPDATE
        } else if(isWaitlisted(userId)){
            JoinButtonStatus.WAITLIST
        } else if(status == RunStatus.CANCELLED || status == RunStatus.COMPLETED || status == RunStatus.PROCESSED){
            JoinButtonStatus.HIDE
        } else {
            JoinButtonStatus.JOIN
        }

    }

    private fun isParticiPant(userId: String): Boolean{
        val findUser = bookingList
            .find { it.userId == userId }

        return  findUser != null
    }

    private fun isWaitlisted(userId: String): Boolean{
        val user = waitList.find { it.userId == userId }
        return  user != null
    }

    fun isDeletable(): Boolean{
        return status == RunStatus.PENDING || status == RunStatus.CONFIRMED
    }

    fun isUpdatable(): Boolean{
        return false
    }

    fun isJoinable(): Boolean{
        return status == RunStatus.PENDING ||
                status == RunStatus.CONFIRMED ||
                status == RunStatus.ONGOING
    }

    fun atFullCapacity(): Boolean{
        return bookingSize() -1 >= maxPlayer
    }


    fun userHasBookingAlready(userId: String): Boolean{
        return bookingList.find {
            it.userId ==userId
        } != null
    }



    fun bookingSize(): Int{
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


    fun updateGuestList(){
        //playersSignedUp = playersSignedUp.filter { !it.isGuestUser }
    }

    enum class JoinButtonStatus{
        JOIN, WAITLIST, UPDATE, HIDE
    }
    class SessionRunBooking(var bookingId:String, var userId: String, var partySize:Int)
}