package com.example.runitup.extensions

import com.example.runitup.model.UserPayment
import com.stripe.model.PaymentMethod

fun PaymentMethod.mapToUserPayment(): UserPayment{
    return UserPayment(this.id, this.type, "", "")
}