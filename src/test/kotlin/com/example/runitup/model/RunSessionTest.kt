package com.example.runitup.model

import com.example.runitup.BaseTest
import com.example.runitup.mobile.enum.RunStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunSessionTest: BaseTest() {

    lateinit var runSession: com.example.runitup.mobile.model.RunSession
    override fun setUp() {
        super.setUp()
        runSession = com.example.runitup.mobile.model.RunSession(
            allowGuest = true,
            date = LocalDate.now(),
            description = "",
            duration = 1,
            endTime = LocalTime.now(),
            hostedBy = null,
            location = null,
            maxGuest = 9,
            maxPlayer = 2,
            notes = "",
            privateRun = true,
            startTime = LocalTime.now(),
            title = "",
            zoneId = ""
        )
    }
    @Test
    fun testIsFree(){
        runSession.courtFee = 10.00
        assertFalse(runSession.isFree())

        runSession.courtFee = 00.00
        assertTrue(runSession.isFree())
    }

    @Test
    fun testIsParticiPant(){
        runSession.bookingList = mutableListOf()
        assertFalse(runSession.isParticiPant("oid"))

        val booking = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid", 2)
        runSession.bookingList = mutableListOf(booking)
        assertTrue(runSession.isParticiPant("oid"))

    }
    @Test
    fun testIsDeletable(){
        runSession.status = RunStatus.PENDING
        assertTrue(runSession.isDeletable())

        runSession.status = RunStatus.CONFIRMED
        assertFalse(runSession.isDeletable())

        runSession.status = RunStatus.ONGOING
        assertFalse(runSession.isDeletable())

        runSession.status = RunStatus.PROCESSED
        assertFalse(runSession.isDeletable())

        runSession.status = RunStatus.COMPLETED
        assertFalse(runSession.isDeletable())

        runSession.status = RunStatus.CANCELLED
        assertFalse(runSession.isDeletable())

    }

    @Test
    fun testIsJoinable(){
        runSession.status = RunStatus.PENDING
        assertTrue(runSession.isJoinable())

        runSession.status = RunStatus.CONFIRMED
        assertTrue(runSession.isJoinable())

        runSession.status = RunStatus.ONGOING
        assertTrue(runSession.isJoinable())

        runSession.status = RunStatus.PROCESSED
        assertFalse(runSession.isJoinable())

        runSession.status = RunStatus.COMPLETED
        assertFalse(runSession.isJoinable())

        runSession.status = RunStatus.CANCELLED
        assertFalse(runSession.isJoinable())

    }

    @Test
    fun testAtFullCapacity(){
        runSession.maxPlayer = 5
        val booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid", 2)
        val booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        assertFalse(runSession.atFullCapacity())

        val booking3 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid", 2)
        runSession.bookingList = mutableListOf(booking1, booking2, booking3)
        assertTrue(runSession.atFullCapacity())


    }

    @Test
    fun testUserHasBookingAlready(){
        runSession.maxPlayer = 5
        val booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid1", 2)
        val booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid2", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        assertTrue(runSession.userHasBookingAlready("oid1"))
        assertFalse(runSession.userHasBookingAlready("oid4"))
    }

    @Test
    fun testAvailableSpots(){
        runSession.maxPlayer = 5
        var booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid1", 2)
        var booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid2", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        assertEquals(2, runSession.availableSpots())


        booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid1", 4)
        booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid2", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        assertEquals(0, runSession.availableSpots())
    }

    @Test
    fun testUpdateTotal(){
        runSession.maxPlayer = 5
        runSession.amount = 10.00
        val booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid1", 2)
        val booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid2", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        runSession.updateTotal()
        assertEquals(30.00, runSession.total)
    }

    @Test
    fun testUpdateStatus(){
        runSession.maxPlayer = 5
        runSession.status = RunStatus.PENDING
        val booking1 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid1", 2)
        val booking2 = com.example.runitup.mobile.model.RunSession.SessionRunBooking("id", "oid2", 1)
        runSession.bookingList = mutableListOf(booking1, booking2)
        runSession.updateStatus("oid1")

        assertEquals( com.example.runitup.mobile.model.RunSession.JoinButtonStatus.UPDATE, runSession.buttonStatus)
        assertEquals( true, runSession.showUpdatePaymentButton)
        assertEquals( true, runSession.guestUpdateAllowed)
        assertEquals( true, runSession.leaveSessionUpdateAllowed)

        runSession.maxPlayer = 3
        runSession.status = RunStatus.CONFIRMED
        runSession.updateStatus("")

        assertEquals( com.example.runitup.mobile.model.RunSession.JoinButtonStatus.WAITLIST, runSession.buttonStatus)
        assertEquals( false, runSession.showUpdatePaymentButton)
        assertEquals( true, runSession.guestUpdateAllowed)
        assertEquals( true, runSession.leaveSessionUpdateAllowed)

        runSession.maxPlayer = 10
        runSession.status = RunStatus.CANCELLED
        runSession.updateStatus("")

        assertEquals( com.example.runitup.mobile.model.RunSession.JoinButtonStatus.HIDE, runSession.buttonStatus)
        assertEquals( false, runSession.showUpdatePaymentButton)
        assertEquals( false, runSession.guestUpdateAllowed)
        assertEquals( false, runSession.leaveSessionUpdateAllowed)


        runSession.maxPlayer = 10
        runSession.status = RunStatus.PENDING
        runSession.updateStatus("")

        assertEquals( com.example.runitup.mobile.model.RunSession.JoinButtonStatus.JOIN, runSession.buttonStatus)
        assertEquals( true, runSession.showUpdatePaymentButton)
        assertEquals( true, runSession.guestUpdateAllowed)
        assertEquals( true, runSession.leaveSessionUpdateAllowed)
    }



}