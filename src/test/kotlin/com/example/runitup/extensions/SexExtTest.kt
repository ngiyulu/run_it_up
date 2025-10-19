package com.example.runitup.extensions

import com.example.runitup.BaseTest
import com.example.runitup.mobile.extensions.mapFromStringToSex
import com.ngiyulu.runitup.messaging.runitupmessaging.model.user.Sex
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SexExtTest: BaseTest() {

    @Test
    fun testMapFromStringToSex(){
        assertEquals(Sex.MALE,  "MALE".mapFromStringToSex())
        assertEquals(Sex.FEMALE,  "FEMALE".mapFromStringToSex())
        assertEquals(Sex.UNKNOWN,  "UNKNOWN".mapFromStringToSex())
        assertEquals(Sex.UNDISCLOSED,  "".mapFromStringToSex())
    }
}