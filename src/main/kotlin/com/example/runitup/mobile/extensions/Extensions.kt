package com.example.runitup.mobile.extensions

import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.stripe.model.PaymentMethod
import java.math.BigDecimal
import java.math.RoundingMode

fun PaymentMethod.mapToUserPayment(isDefaultPayment:Boolean): CardModel {
    val card = this.card
    return CardModel(
        id = this.id,
        brand = card.brand,
        last4 = card.last4,
        expMonth = card.expMonth.toInt(),
        expYear = card.expYear.toInt(),
        isDefault = isDefaultPayment
    )
}

//amount  * 100 converts 10.99 to 1099cents
fun Double.convertToCents(): Long{
    // Round to 2 decimals using HALF_UP, then shift 2 places to get cents
    return BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
}

inline fun <A : Any, B : Any, R> let(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) block(a, b) else null

inline fun <A : Any, B : Any, C : Any, R> let(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) block(a, b, c) else null