package com.example.runitup.extensions

import com.example.runitup.BaseTest
import com.stripe.model.PaymentMethod
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ExtensionsTest: BaseTest() {

    private val paymentMethod = mock<PaymentMethod>()
    private val paymentCard = mock<PaymentMethod.Card>()


    override fun setUp() {
        super.setUp()
        whenever(paymentMethod.id).thenReturn("id")
        whenever(paymentMethod.card).thenReturn(paymentCard)
        whenever(paymentCard.last4).thenReturn("last4" )
        whenever(paymentCard.brand).thenReturn("brand")
        whenever(paymentCard.expMonth).thenReturn(10)
        whenever(paymentCard.expYear).thenReturn (2024)
    }
    @Test
    fun testMapToUserPayment(){
        var data = paymentMethod.mapToUserPayment(true)
        assertEquals(data.last4, "last4")
        assertEquals(data.brand, "brand")
        assertEquals(data.expMonth, 10)
        assertEquals(data.expYear, 2024)
        assertEquals(data.isDefault, true)


        data = paymentMethod.mapToUserPayment(false)
        assertEquals(data.last4, "last4")
        assertEquals(data.brand, "brand")
        assertEquals(data.expMonth, 10)
        assertEquals(data.expYear, 2024)
        assertEquals(data.isDefault, false)
    }

    @Test
    fun testConvertToCents(){
        assertEquals(1099, 10.99.convertToCents())
        assertEquals(859, 08.59.convertToCents())
    }


}