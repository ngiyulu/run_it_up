package com.example.runitup.mobile.model

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.rest.v1.dto.EncryptedCodeModel
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.web.dto.Role
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
    var gym: Gym? = null,
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    var location: GeoJsonPoint?,
    val date: LocalDate,      // local calendar date at the venue
    var startTime: LocalTime,      // local time at the venue
    var endTime: LocalTime,      // local time at the venue
    val zoneId: String,       // IANA zone, e.g. "America/Chicago"
    val startAtUtc: Instant? = null, // optional cache
    var oneHourNotificationSent: Boolean = false,
    //AdminUser Id
    var hostedBy: String?,
    var host:AdminUser? = null,
    var allowGuest: Boolean,
    var duration: Int, // in hours
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var amount: Double = 0.0,
    var manualFee:Double = 0.0,
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
    var showStartButton: Boolean = false,
    //this flag determines whether or not admin is allowed to mark payment as paid manually
    var shouldShowPayButton: Boolean = false,
    var guestUpdateAllowed: Boolean = true,
    var leaveSessionUpdateAllowed: Boolean = true,
    var confirmedAt:Instant? = null,
    var statusBeforeCancel: RunStatus = RunStatus.PENDING,
    // show remove button on the web portal
    var showRemoveButton: Boolean = true,
    var cancellation: Cancellation? = null,
    var startedAt:Instant? = null,
    var startedBy:String? = null,
    var completedAt:Instant? = null,
    var code: EncryptedCodeModel? = null,
    var plain:String? = null,
    var adminStatus: AdminStatus = AdminStatus.OTHER,
    var bookingPaymentState: BookingPaymentState? = null,
    var contact:String
): BaseModel(){

    fun isSessionFree(): Boolean{
        return amount == 0.0
    }
    fun updateStatus(){
        isFree = isSessionFree()
        showStartButton = status == RunStatus.CONFIRMED
    }
    fun updateStatus(userId: String){
        isFree = isSessionFree()
        shouldShowPayButton = isFree && status == RunStatus.PENDING || status == RunStatus.ONGOING || status == RunStatus.CONFIRMED
        isFull = bookings.size == maxPlayer
        buttonStatus = if(status == RunStatus.CANCELLED || status == RunStatus.ONGOING || status == RunStatus.COMPLETED || status == RunStatus.PROCESSED){
            JoinButtonStatus.HIDE
        } else if(isParticiPant(userId)){
            JoinButtonStatus.UPDATE
        } else if(atFullCapacity()){
            if(isWaitlisted(userId)){
                JoinButtonStatus.WAITLIST
            }
            else if(isParticiPant(userId)) JoinButtonStatus.HIDE
            else {
                JoinButtonStatus.WAITLIST
            }

        } else {
            JoinButtonStatus.JOIN
        }
        val isPendingOrConfirmed = status == RunStatus.PENDING || status == RunStatus.CONFIRMED
        userStatus = if(isParticiPant(userId)){
            UserButtonStatus.PARTICIPANT
        } else if(isWaitlisted(userId)){
            UserButtonStatus.WAITLISTED
        } else {
            UserButtonStatus.NONE
        }
        showUpdatePaymentButton = status == RunStatus.PENDING
        guestUpdateAllowed = isPendingOrConfirmed && !isFull
        leaveSessionUpdateAllowed = isPendingOrConfirmed
    }

    private fun isParticiPant(userId: String): Boolean{
        val findUser = bookingList
            .find { it.userId == userId }

        return  findUser != null
    }

    fun updateAdmin(adminUser: AdminUser){
        adminStatus = if(adminUser.role == Role.SUPER_ADMIN){
            if(hostedBy == adminUser.id){
                AdminStatus.SUPER_ADMIN_MINE
            } else{
                AdminStatus.SUPER_ADMIN
            }
        } else if(hostedBy == adminUser.id){
            AdminStatus.MINE
        } else{
            AdminStatus.OTHER
        }
    }

    fun getBooking(userId: String): SessionRunBooking? {
        return bookingList
            .find { it.userId == userId }
    }


    fun isWaitlisted(userId: String): Boolean{

        val waitListed = waitList
            .find { it.userId == userId }

        return  waitListed != null
    }

    fun isDeletable(): Boolean{
        return status != RunStatus.CANCELLED || status != RunStatus.COMPLETED || status != RunStatus.PROCESSED
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

    enum class AdminStatus{
        MINE, OTHER, SUPER_ADMIN, SUPER_ADMIN_MINE
    }
    class SessionRunBooking(var bookingId:String, var userId: String, var partySize:Int)

    class Cancellation(val canceledBy:String,val cancelledAt:Instant, val reason:String)
}