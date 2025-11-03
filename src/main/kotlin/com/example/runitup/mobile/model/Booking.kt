package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.rest.v1.dto.RunUser
import org.bson.types.ObjectId
import java.time.Instant

class Booking (
    var id: String? = ObjectId().toString(),
    //party size include the guest and the user
    var partySize: Int = 1,
    // created userId to make query easier
    var userId: String,
    var user: RunUser,
    var runSessionId: String,
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    var sessionAmount: Double,
    var total: Double,
    var paymentId:String? = null,
    // setupIntentId is the payment you sent up when you are waitlisted
    var setupIntentId:String? = null,
    var paymentMethodId:String? = null,
    var checkInNumber: Int = 0,
    var joinedAtFromWaitList: Instant?,
    var status: BookingStatus = BookingStatus.JOINED,
    var isLocked:Boolean = false,
    var currentTotalCents: Long = 0L,
    var isLockedAt: Instant? = null,
    var promotedAt: Instant? = null,
    var bookingPaymentState: BookingPaymentState? = null
): BaseModel(){

    fun getNumOfGuest(): Int{
        return partySize - 1
    }
}
class  BookingPayment(var amount: Double, var stripePaymentId:String,  var paymentStatus: PaymentStatus = PaymentStatus.PENDING)
enum class BookingStatus{
    JOINED, WAITLISTED, CANCELLED
}