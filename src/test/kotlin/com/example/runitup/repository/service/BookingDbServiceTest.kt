package com.example.runitup.repository.service

import com.example.runitup.BaseTest
import com.example.runitup.model.Booking
import com.mongodb.client.result.DeleteResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.query.Query
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookingDbServiceTest: BaseTest() {

    private lateinit var bookingDbService: BookingDbService
    override fun setUp() {
        super.setUp()
        bookingDbService = BookingDbService()
        bookingDbService.bookingRepository = mockBookingRepository
        bookingDbService.mongoTemplate = mockMongoTemplate
    }

    @Test
    fun testCancelUserBooking(){
        val result = mock<DeleteResult>()
        whenever(result.deletedCount).thenReturn(0)
        val query = argumentCaptor<Query>()
        whenever(mockMongoTemplate.remove(query.capture(), eq(Booking::class.java))).thenReturn(result)
        assertFalse(bookingDbService.cancelUserBooking(""))

        assertEquals("Query: { \"userId\" : \"\"}, Fields: {}, Sort: {}", query.firstValue.toString())

        whenever(result.deletedCount).thenReturn(1)
        assertTrue(bookingDbService.cancelUserBooking(""))
    }


    @Test
    fun testGetBooking(){
        val query = argumentCaptor<Query>()
        whenever(mockMongoTemplate.findOne(query.capture(), eq(Booking::class.java))).thenReturn(null)

        assertNull(bookingDbService.getBooking("user", "session"))
        assertEquals("Query: { \"userId\" : \"user\", \"runSessionId\" : \"session\"}, Fields: {}, Sort: {}", query.firstValue.toString())

    }

    @Test
    fun testCancelAllBooking(){
        val result = mock<DeleteResult>()
        whenever(result.deletedCount).thenReturn(0)
        val query = argumentCaptor<Query>()
        whenever(mockMongoTemplate.remove(query.capture(), eq(Booking::class.java))).thenReturn(result)
        assertFalse(bookingDbService.cancelAllBooking("run"))

        assertEquals("Query: { \"runSessionId\" : \"run\"}, Fields: {}, Sort: {}", query.firstValue.toString())

        whenever(result.deletedCount).thenReturn(1)
        assertTrue(bookingDbService.cancelAllBooking("run"))
    }

}