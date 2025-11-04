package com.example.runitup.extensions

import com.example.runitup.BaseTest
import com.example.runitup.mobile.enum.SkillLevel
import com.example.runitup.mobile.extensions.mapFromString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SkillLevelExtTest: BaseTest() {

    @Test
    fun testMapFromStringToSex(){
        assertEquals(SkillLevel.BEGINNER,  "BEGINNER".mapFromString())
        assertEquals(SkillLevel.COMPETITIVE,  "COMPETITIVE".mapFromString())
        assertEquals(SkillLevel.COLLEGIATE,  "COLLEGIATE".mapFromString())
        assertEquals(SkillLevel.PRO,  "PRO".mapFromString())
        assertEquals(SkillLevel.BEGINNER,  "".mapFromString())
    }
}