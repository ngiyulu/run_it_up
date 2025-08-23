package com.example.runitup.model

import com.example.runitup.dto.RunUser
import com.example.runitup.enum.PaymentStatus
import org.bson.types.ObjectId

class Booking (
    var id: String? = ObjectId().toString(),
    //party size include the guest and the user
    var partySize: Int = 1,
    // created userId to make query easier
    var userId: String,
    var user: RunUser,
    var runSessionId: String,
    // this is the object that gets created when we join a run
    // a new object will be made for each request to join
    var stripePayment:List<BookingPayment>,
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    var sessionAmount: Double,
    var total: Double,
    var checkInNumber: Int = 0
): BaseModel()
class  BookingPayment(var amount: Double, var stripePaymentId:String,  var paymentStatus: PaymentStatus = PaymentStatus.PENDING)