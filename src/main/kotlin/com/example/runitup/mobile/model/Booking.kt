package com.example.runitup.mobile.model

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.rest.v1.dto.RunUser
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
    var stripePayment:List<com.example.runitup.mobile.model.BookingPayment>,
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    var sessionAmount: Double,
    var total: Double,
    var checkInNumber: Int = 0
): com.example.runitup.mobile.model.BaseModel()
class  BookingPayment(var amount: Double, var stripePaymentId:String,  var paymentStatus: PaymentStatus = PaymentStatus.PENDING)