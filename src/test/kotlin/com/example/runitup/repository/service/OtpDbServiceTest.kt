package com.example.runitup.repository.service

import com.example.runitup.BaseTest
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.Otp
import com.example.runitup.mobile.repository.service.OtpDbService
import com.mongodb.client.result.DeleteResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpDbServiceTest: BaseTest() {

    private lateinit var service: OtpDbService
    override fun setUp() {
        super.setUp()
        service = OtpDbService()
        service.otpRepository = mockOtpRepository
        service.mongoTemplate = mockMongoTemplate
    }

//    @Test
//    fun testCancelUserBooking(){
//        val result = mock<DeleteResult>()
//        whenever(result.deletedCount).thenReturn(0)
//        val query = argumentCaptor<Query>()
//        whenever(mockMongoTemplate.remove(query.capture(), eq(Booking::class.java))).thenReturn(result)
//        assertFalse(bookingDbService.cancelUserBooking(""))
//
//        assertEquals("Query: { \"userId\" : \"\"}, Fields: {}, Sort: {}", query.firstValue.toString())
//
//        whenever(result.deletedCount).thenReturn(1)
//        assertTrue(bookingDbService.cancelUserBooking(""))
//    }
//
//
//    @Test
//    fun testGetBooking(){
//        val query = argumentCaptor<Query>()
//        whenever(mockMongoTemplate.findOne(query.capture(), eq(Booking::class.java))).thenReturn(null)
//
//        assertNull(bookingDbService.getBooking("user", "session"))
//        assertEquals("Query: { \"userId\" : \"user\", \"runSessionId\" : \"session\"}, Fields: {}, Sort: {}", query.firstValue.toString())
//
//    }
//
//    @Test
//    fun testCancelAllBooking(){
//        val result = mock<DeleteResult>()
//        whenever(result.deletedCount).thenReturn(0)
//        val query = argumentCaptor<Query>()
//        whenever(mockMongoTemplate.remove(query.capture(), eq(Booking::class.java))).thenReturn(result)
//        assertFalse(bookingDbService.cancelAllBooking("run"))
//
//        assertEquals("Query: { \"runSessionId\" : \"run\"}, Fields: {}, Sort: {}", query.firstValue.toString())
//
//        whenever(result.deletedCount).thenReturn(1)
//        assertTrue(bookingDbService.cancelAllBooking("run"))
//    }
//
    @Test fun testDisableOtp(){
        val otp = com.example.runitup.mobile.model.Otp(null, "code", null, "", null)
        whenever(mockOtpRepository.save(otp)).thenReturn(otp)

        service.disableOtp(otp)

        verify(mockOtpRepository).save(otp)
    }

    @Test
    fun getOtp(){
        val query = argumentCaptor<Query>()
        whenever(mockMongoTemplate.findOne(query.capture(), eq(com.example.runitup.mobile.model.Otp::class.java))).thenReturn(null)

        assertNull(service.getOtp("760-571-7457"))
        assertEquals("Query: { \"phoneNumber\" : \"760-571-7457\", \"isActive\" : true}, Fields: {}, Sort: {}", query.firstValue.toString())

    }

    @Test
    fun testGenerateOtp(){
        val otp = com.example.runitup.mobile.model.Otp(null, "code", null, "", null)
        val query = argumentCaptor<Query>()
        val updateQuery = argumentCaptor<Update>()
        val otpArg = argumentCaptor<com.example.runitup.mobile.model.Otp>()

        whenever(mockOtpRepository.save(otpArg.capture())).thenReturn(otp)

        service.generateOtp("oid", "760")

        verify(mockMongoTemplate).updateFirst(query.capture(), updateQuery.capture(), eq( com.example.runitup.mobile.model.Otp::class.java))
        assertEquals("Query: { \"phoneNumber\" : \"760\"}, Fields: {}, Sort: {}", query.firstValue.toString())
        assertEquals("{ \"\$set\" : { \"isActive\" : false}}", updateQuery.firstValue.toString())
        val otpCreated = otpArg.firstValue
        assertEquals("760", otpCreated.phoneNumber)
        assertEquals("oid", otpCreated.userId)
        assertEquals(4, otpCreated.code.length)

    }
}