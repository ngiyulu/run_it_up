package com.example.runitup.utility

import com.example.runitup.BaseTest
import com.example.runitup.mobile.utility.AgeUtil
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AgeUtilTest: BaseTest() {


    override fun setUp() {
        super.setUp()

    }
    @Test
    fun testGetAge(){
        val age = AgeUtil.ageFrom("1993-10-01", "America/Los_Angeles")
        assertEquals(31, age)
    }
}