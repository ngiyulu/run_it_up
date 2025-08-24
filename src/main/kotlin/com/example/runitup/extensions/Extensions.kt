package com.example.runitup.extensions

import com.stripe.model.PaymentMethod
import java.math.BigDecimal
import java.math.RoundingMode

fun PaymentMethod.mapToUserPayment(isDefaultPayment:Boolean): com.example.runitup.web.rest.v1.dto.payment.CardModel {
    val card = this.card
    return com.example.runitup.web.rest.v1.dto.payment.CardModel(
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
    return this.roundNumber() * 100
}
private fun Double.roundNumber(): Long{
    val bd = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)
    return  bd.toLong()
}
inline fun <A : Any, B : Any, R> let(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) block(a, b) else null

inline fun <A : Any, B : Any, C : Any, R> let(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) block(a, b, c) else null