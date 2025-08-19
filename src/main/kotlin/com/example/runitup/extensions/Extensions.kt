package com.example.runitup.extensions

import com.example.runitup.dto.CardModel
import com.stripe.model.PaymentMethod

fun PaymentMethod.mapToUserPayment(): CardModel{
    val card = this.card
    return CardModel(
        id = this.id,
        brand = card.brand,
        last4 = card.last4,
        expMonth = card.expMonth.toInt(),
        expYear = card.expYear.toInt()
    )
}

inline fun <A : Any, B : Any, R> let(a: A?, b: B?, block: (A, B) -> R): R? =
    if (a != null && b != null) block(a, b) else null

inline fun <A : Any, B : Any, C : Any, R> let(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) block(a, b, c) else null