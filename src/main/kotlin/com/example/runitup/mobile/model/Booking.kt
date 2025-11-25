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
    var cancelledAt:Instant? = null,
    var cancelledBy:String? = null,
    var isLocked:Boolean = false,
    var currentTotalCents: Long = 0L,
    var isLockedAt: Instant? = null,
    var promotedAt: Instant? = null,
    var customerId:String? = null,
    var paymentAuthorization: List<PaymentAuthorization> = emptyList(),
    var bookingPaymentState: BookingPaymentState? = null,
    var completedAt:Instant? = null,
    var paidAt:Instant? = null,
    var date:String
): BaseModel(){

    fun getNumOfGuest(): Int{
        return partySize - 1
    }
    fun isPaymentStatusManualUpdateAllowed(): Boolean{
        return status == BookingStatus.JOINED
    }
}
class  BookingPayment(var amount: Double, var stripePaymentId:String,  var paymentStatus: PaymentStatus = PaymentStatus.PENDING)
enum class BookingStatus{
    JOINED, WAITLISTED, CANCELLED, COMPLETED
}
fun getAllBookingStatuses(): List<BookingStatus> {
    return enumValues<BookingStatus>().toList()
}