package com.example.runitup

import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.OtpRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.mongodb.core.MongoTemplate


@ExtendWith(MockitoExtension::class)
open class BaseTest {

    @Mock
    lateinit var mockBookingRepository: BookingRepository

    @Mock
    lateinit var mockOtpRepository: OtpRepository

    @Mock
    lateinit var mockMongoTemplate: MongoTemplate

    @Mock
    lateinit var mockRSession: com.example.runitup.mobile.model.RunSession

    open fun setUp(){
        var a = 1
    }

    @BeforeEach
    fun start(){
        setUp()
    }
}