package com.example.runitup.extensions

import com.example.runitup.dto.payment.CardModel
import com.stripe.model.PaymentMethod

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

inline fun <A : Any, B : Any, R> let(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) block(a, b) else null

inline fun <A : Any, B : Any, C : Any, R> let(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) block(a, b, c) else null