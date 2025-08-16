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
    var time: LocalTime,      // local time at the venue
    val zoneId: String,       // IANA zone, e.g. "America/Chicago"
    val startAtUtc: Instant? = null, // optional cache
    var hostedBy: String?,
    var duration: Int, // in hours
    var amount: Double = 0.0,
    var total: Double = 0.0,
    var maxPlayer: Int,
    var playersSignedUp: MutableList<RunUser> = mutableListOf(),
    var waitList: MutableList<RunUser> = mutableListOf(),
    var players:  MutableList<User>? = null,
    var courtFee: Double = 0.0,
    var maxGuest: Int,
    var payment: RunSessionPayment?,
    var status: RunStatus = RunStatus.PENDING
): BaseModel(){


    fun getPlayersList():List<RunUser>{
        return playersSignedUp.filter { it.status == RunUser.RunUserStatus.GOING }
    }
    fun updateTotal(){
        total = getPlayersList().sumOf { amount + (it.guest * amount)}
    }
    fun isDeletable(): Boolean{
        return status == RunStatus.PENDING || status == RunStatus.CONFIRMED
    }

    fun isUpdatable(): Boolean{
        return false
    }

    fun atFullCapacity(): Boolean{
        return getPlayersList().size -1 >= maxPlayer
    }

    fun atFullCapacityForGuest(guest: Int): Boolean{
        return getPlayersList().size  -1 + guest >= maxPlayer
    }
    fun updatePlayersFromWaitList(){
        while (waitList.isNotEmpty() || !atFullCapacity()){
            val firstOnWaitList = waitList[0]
            firstOnWaitList.status = RunUser.RunUserStatus.GOING
            playersSignedUp.add(firstOnWaitList)
            waitList.removeAt(0)
        }

    }

    fun availableSpots(): Int{
        return  maxPlayer - (getPlayersList().size -1)
    }
}