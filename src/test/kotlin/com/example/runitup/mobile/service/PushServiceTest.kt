//// src/test/kotlin/com/example/runitup/mobile/service/PushServiceTest.kt
//package com.example.runitup.mobile.service
//
//import com.example.runitup.mobile.enum.PhoneType
//import com.example.runitup.mobile.model.Phone
//import com.example.runitup.mobile.push.PushGateway
//import com.example.runitup.mobile.repository.PhoneRepository
//import com.example.runitup.mobile.rest.v1.dto.PushNotification
//import com.example.runitup.mobile.rest.v1.dto.PushResult
//import io.mockk.*
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//
//class PushServiceTest {
//
//    private lateinit var service: PushService
//    private val phoneRepo = mockk<PhoneRepository>(relaxed = true)
//    private val fcm = mockk<PushGateway>()
//    private val apns = mockk<PushGateway>()
//    private val notif = PushNotification(title = "t", body = "b", data = mapOf("k" to "v"))
//
//    private val HARDCODED_IOS_TOKEN =
//        "5cd69584eb0b0f0fcaaa12c96c0ee6ad4ff0ec90d155374065e00ff217ac6e8d"
//
//    @BeforeEach
//    fun setUp() {
//        service = PushService(phoneRepo, this@PushServiceTest.fcm, this@PushServiceTest.apns, false)
//        clearMocks(phoneRepo, fcm, apns)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        clearAllMocks()
//        unmockkAll()
//    }
//
//    @Test
//    fun `sendToPhone ANDROID routes to FCM`() {
//        val phone = Phone(os = "Android", model = "X", userId = "u1", token = "tokA1", phoneId = "p1", type = PhoneType.ANDROID)
//        every { fcm.sendToTokens(listOf("tokA1"), notif) } returns PushResult(1,1,0, emptyList(), emptyList())
//
//        val res = service.sendToPhone(phone, notif)
//
//        assertThat(res.requested).isEqualTo(1)
//        verify(exactly = 1) { fcm.sendToTokens(listOf("tokA1"), notif) }
//        verify(exactly = 0) { apns.sendToTokens(any(), any()) }
//        verify(exactly = 0) { phoneRepo.saveAll(any<List<Phone>>()) }
//    }
//
//    @Test
//    fun `sendToPhone iOS uses FCM when useApnsDirect is false`() {
//        val phone = Phone(os = "iOS", model = "iPhone", userId = "u2", token = "tokI1", phoneId = "p2", type = PhoneType.IOS)
//        service.useApnsDirect = false
//        every { fcm.sendToTokens(listOf("tokI1"), notif) } returns PushResult(1,1,0, emptyList(), emptyList())
//
//        val res = service.sendToPhone(phone, notif)
//
//        assertThat(res.success).isEqualTo(1)
//        verify(exactly = 1) { fcm.sendToTokens(listOf("tokI1"), notif) }
//        verify(exactly = 0) { apns.sendToTokens(any(), any()) }
//    }
//
//    @Test
//    fun `sendToPhone iOS uses APNS when useApnsDirect is true`() {
//        val phone = Phone(os = "iOS", model = "iPhone", userId = "u3", token = "tokI2", phoneId = "p3", type = PhoneType.IOS)
//        service.useApnsDirect = true
//        every { apns.sendToTokens(listOf("tokI2"), notif) } returns PushResult(1,1,0, emptyList(), emptyList())
//
//        val res = service.sendToPhone(phone, notif)
//
//        assertThat(res.success).isEqualTo(1)
//        verify(exactly = 1) { apns.sendToTokens(listOf("tokI2"), notif) }
//        verify(exactly = 0) { fcm.sendToTokens(any(), any()) }
//    }
//
//    @Test
//    fun `sendToPhones uses FCM for Android tokens and FCM for iOS (default), with hardcoded iOS token`() {
//        service.useApnsDirect = false
//
//        val a1 = Phone("Android", "M1", "uA1", "aTok1", "pa1", "", PhoneType.ANDROID)
//        val a2 = Phone("Android", "M2", "uA2", "aTok2", "pa2", "", PhoneType.ANDROID)
//        val i1 = Phone("iOS", "iP1", "uI1", "iTok1", "pi1", "", PhoneType.IOS)
//        val i2 = Phone("iOS", "iP2", "uI2", "iTok2", "pi2", "", PhoneType.IOS)
//
//        val androidTokens = listOf("aTok1", "aTok2")
//
//        val androidResult = PushResult(requested = 2, success = 2, failed = 0, invalidTokens = emptyList(), errors = emptyList())
//        val iosResult = PushResult(requested = 1, success = 1, failed = 0, invalidTokens = emptyList(), errors = emptyList())
//
//        // IMPORTANT: distinct stubs per argument to avoid one stub shadowing both calls
//        every { fcm.sendToTokens(eq(androidTokens), notif) } returns androidResult
//        every { fcm.sendToTokens(eq(listOf(HARDCODED_IOS_TOKEN)), notif) } returns iosResult
//
//        val res = service.sendToPhones(listOf(a1, a2, i1, i2), notif)
//
//        assertThat(res.requested).isEqualTo(3) // 2 android + 1 iOS
//        assertThat(res.success).isEqualTo(3)
//        assertThat(res.failed).isEqualTo(0)
//
//        verify(exactly = 1) { fcm.sendToTokens(eq(androidTokens), notif) }
//        verify(exactly = 1) { fcm.sendToTokens(eq(listOf(HARDCODED_IOS_TOKEN)), notif) }
//        verify(exactly = 0) { apns.sendToTokens(any(), any()) }
//        verify(exactly = 0) { phoneRepo.saveAll(any<List<Phone>>()) }
//    }
//
//    @Test
//    fun `sendToPhones uses FCM for Android and APNS for iOS when useApnsDirect=true (still hardcoded iOS token)`() {
//        service.useApnsDirect = true
//
//        val a = Phone("Android", "M", "uA", "aTok", "pa", "", PhoneType.ANDROID)
//        val i = Phone("iOS", "iP", "uI", "iTok", "pi", "", PhoneType.IOS)
//
//        val androidResult = PushResult(1,1,0, emptyList(), emptyList())
//        val iosResult = PushResult(1,1,0, emptyList(), emptyList())
//
//        every { fcm.sendToTokens(eq(listOf("aTok")), notif) } returns androidResult
//        every { apns.sendToTokens(eq(listOf(HARDCODED_IOS_TOKEN)), notif) } returns iosResult
//
//        val res = service.sendToPhones(listOf(a, i), notif)
//
//        assertThat(res.requested).isEqualTo(2)
//        assertThat(res.success).isEqualTo(2)
//        assertThat(res.failed).isEqualTo(0)
//
//        verify(exactly = 1) { fcm.sendToTokens(eq(listOf("aTok")), notif) }
//        verify(exactly = 1) { apns.sendToTokens(eq(listOf(HARDCODED_IOS_TOKEN)), notif) }
//        verify(exactly = 0) { phoneRepo.saveAll(any<List<Phone>>()) }
//    }
//
//    @Test
//    fun `sendToPhones aggregates invalid tokens but does not persist changes`() {
//        service.useApnsDirect = false
//
//        val a = Phone("Android", "M", "uA", "aTok", "pa", "", PhoneType.ANDROID)
//        val i = Phone("iOS", "iP", "uI", "iTok", "pi","",  PhoneType.IOS)
//
//        val androidResult = PushResult(1,1,0, invalidTokens = listOf("aTok"), errors = emptyList())
//        val iosResult = PushResult(1,0,1, invalidTokens = listOf("iTokBAD"), errors = listOf("apns-fail"))
//
//        every { fcm.sendToTokens(eq(listOf("aTok")), notif) } returns androidResult
//        every { fcm.sendToTokens(eq(listOf(HARDCODED_IOS_TOKEN)), notif) } returns iosResult
//
//        val res = service.sendToPhones(listOf(a, i), notif)
//
//        assertThat(res.invalidTokens).containsExactlyInAnyOrder("aTok", "iTokBAD")
//        assertThat(res.errors).containsExactly("apns-fail")
//
//        verify(exactly = 0) { phoneRepo.saveAll(any<List<Phone>>()) }
//        verify(exactly = 0) { phoneRepo.findAllByTokenIn(any()) }
//    }
//}
