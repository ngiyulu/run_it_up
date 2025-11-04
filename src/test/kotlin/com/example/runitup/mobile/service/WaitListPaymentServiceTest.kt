// src/test/kotlin/com/example/runitup/mobile/service/WaitListPaymentServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.SetupStatus
import com.example.runitup.mobile.model.WaitlistSetupState
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.param.SetupIntentCancelParams
import com.stripe.param.SetupIntentCreateParams
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WaitListPaymentServiceTest {

    private lateinit var repo: WaitlistSetupStateRepository
    private lateinit var service: WaitListPaymentService

    @BeforeEach
    fun setUp() {
        repo = mockk(relaxed = true)
        service = WaitListPaymentService().apply { this.waitlistSetupRepo = repo }
        mockkStatic(SetupIntent::class)
        mockkStatic(PaymentIntent::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SetupIntent::class)
        unmockkStatic(PaymentIntent::class)
        clearAllMocks()
    }

    private fun mkSetupIntent(id: String, status: String, clientSecret: String? = null): SetupIntent {
        val si = mockk<SetupIntent>()
        every { si.id } returns id
        every { si.status } returns status
        every { si.clientSecret } returns clientSecret
        return si
    }

    @Test
    fun `ensureWaitlistCardReady - succeeded inserts SUCCEEDED row, no notify`() {
        val si = mkSetupIntent("seti_1", "succeeded")
        every { SetupIntent.create(any<SetupIntentCreateParams>()) } returns si

        every { repo.findByBookingIdAndPaymentMethodId("b1", "pm_1") } returns null

        val insertSlot = slot<WaitlistSetupState>()
        // --- Disambiguate overloads explicitly ---
        every { repo.insert(any<Iterable<WaitlistSetupState>>()) } throws AssertionError("batch insert should not be called")
        every { repo.insert(capture(insertSlot)) } answers { firstArg() }
        // save(S) vs saveAll(Iterable<S>) — be explicit as well
        every { repo.save(any<WaitlistSetupState>()) } answers { firstArg() }
        every { repo.saveAll(any<Iterable<WaitlistSetupState>>()) } answers { firstArg() }

        val res = service.ensureWaitlistCardReady(
            bookingId = "b1", sessionId = "s1", userId = "u1",
            customerId = "cus_1", paymentMethodId = "pm_1"
        )

        assertThat(res.status).isEqualTo(SetupStatus.SUCCEEDED)
        assertThat(res.needsUserAction).isFalse()
        assertThat(res.setupIntentId).isEqualTo("seti_1")

        val row = insertSlot.captured
        assertThat(row.bookingId).isEqualTo("b1")
        assertThat(row.sessionId).isEqualTo("s1")
        assertThat(row.userId).isEqualTo("u1")
        assertThat(row.customerId).isEqualTo("cus_1")
        assertThat(row.paymentMethodId).isEqualTo("pm_1")
        assertThat(row.setupIntentId).isEqualTo("seti_1")
        assertThat(row.needsUserAction).isFalse()

        verify(exactly = 0) { repo.save(any<WaitlistSetupState>()) } // no notify bump
    }

    @Test
    fun `ensureWaitlistCardReady - requires_action inserts row and saves notify bump`() {
        val si = mkSetupIntent("seti_need_action", "requires_action", clientSecret = "sec_123")
        every { SetupIntent.create(any<SetupIntentCreateParams>()) } returns si

        every { repo.findByBookingIdAndPaymentMethodId("b2", "pm_2") } returns null

        val insertSlot = slot<WaitlistSetupState>()
        every { repo.insert(any<Iterable<WaitlistSetupState>>()) } throws AssertionError("batch insert should not be called")
        every { repo.insert(capture(insertSlot)) } answers { firstArg() }
        every { repo.save(any<WaitlistSetupState>()) } answers { firstArg() }
        every { repo.saveAll(any<Iterable<WaitlistSetupState>>()) } answers { firstArg() }

        val res = service.ensureWaitlistCardReady(
            bookingId = "b2", sessionId = "s2", userId = "u2",
            customerId = "cus_2", paymentMethodId = "pm_2"
        )

        assertThat(res.status).isEqualTo(SetupStatus.REQUIRES_ACTION)
        assertThat(res.needsUserAction).isTrue()
        assertThat(res.clientSecret).isEqualTo("sec_123")

        val inserted = insertSlot.captured
        assertThat(inserted.setupIntentId).isEqualTo("seti_need_action")
        assertThat(inserted.needsUserAction).isTrue()

        // a save should happen to bump lastNotifiedAt
        verify(atLeast = 1) { repo.save(any<WaitlistSetupState>()) }
    }

    @Test
    fun `ensureWaitlistCardReady - existing row is updated (save, not insert)`() {
        val si = mkSetupIntent("seti_existing", "succeeded")
        every { SetupIntent.create(any<SetupIntentCreateParams>()) } returns si

        val existing = WaitlistSetupState(
            id = "row_1", bookingId = "b3", sessionId = "s3",
            userId = "u3", customerId = "cus_3", paymentMethodId = "pm_3",
            setupIntentId = "old_si", status = SetupStatus.UNKNOWN
        )
        every { repo.findByBookingIdAndPaymentMethodId("b3", "pm_3") } returns existing

        val saveSlot = slot<WaitlistSetupState>()
        every { repo.insert(any<Iterable<WaitlistSetupState>>()) } throws AssertionError("batch insert should not be called")
        every { repo.insert(any<WaitlistSetupState>()) } throws AssertionError("single insert should not be called")
        every { repo.save(capture(saveSlot)) } answers { firstArg() }
        every { repo.saveAll(any<Iterable<WaitlistSetupState>>()) } answers { firstArg() }

        val res = service.ensureWaitlistCardReady(
            bookingId = "b3", sessionId = "s3", userId = "u3",
            customerId = "cus_3", paymentMethodId = "pm_3"
        )

        assertThat(res.status).isEqualTo(SetupStatus.SUCCEEDED)
        verify(exactly = 0) { repo.insert(any<WaitlistSetupState>()) }
        verify(exactly = 0) { repo.insert(any<Iterable<WaitlistSetupState>>()) }
        verify(exactly = 1) { repo.save(any<WaitlistSetupState>()) }

        val saved = saveSlot.captured
        assertThat(saved.id).isEqualTo("row_1")
        assertThat(saved.setupIntentId).isEqualTo("seti_existing")
        assertThat(saved.status).isEqualTo(SetupStatus.SUCCEEDED)
    }

    @Test
    fun `refreshWaitlistSetupState pulls SI and updates row`() {
        val setupIntentId = "seti_refresh"
        val si = mkSetupIntent(setupIntentId, "succeeded")
        every { SetupIntent.retrieve(setupIntentId) } returns si

        val row = WaitlistSetupState(
            id = "rowX", bookingId = "bX", sessionId = "sX",
            userId = "uX", customerId = "cus_X", paymentMethodId = "pm_X",
            setupIntentId = setupIntentId, status = SetupStatus.UNKNOWN,
            needsUserAction = true, clientSecret = "old_client"
        )
        every { repo.findBySetupIntentId(setupIntentId) } returns row

        val saveSlot = slot<WaitlistSetupState>()
        every { repo.save(capture(saveSlot)) } answers { firstArg() }

        val updated = service.refreshWaitlistSetupState(setupIntentId)

        assertThat(updated).isNotNull()
        assertThat(updated!!.status).isEqualTo(SetupStatus.SUCCEEDED)
        assertThat(updated.needsUserAction).isFalse()
        assertThat(updated.clientSecret).isNull()
        verify(exactly = 1) { repo.save(any<WaitlistSetupState>()) }
    }

    @Test
    fun `cancelWaitlistSetupIntent cancels on Stripe and persists CANCELED`() {
        val setupIntentId = "seti_cancel"
        val si = mkSetupIntent(setupIntentId, "requires_action")
        val siCanceled = mkSetupIntent(setupIntentId, "canceled")

        every { SetupIntent.retrieve(setupIntentId) } returns si
        every { si.cancel(any<SetupIntentCancelParams>()) } returns siCanceled

        val row = WaitlistSetupState(
            id = "rowC", bookingId = "bC", sessionId = "sC",
            userId = "uC", customerId = "cus_C", paymentMethodId = "pm_C",
            setupIntentId = setupIntentId
        )
        every { repo.findBySetupIntentId(setupIntentId) } returns row

        val saveSlot = slot<WaitlistSetupState>()
        every { repo.save(capture(saveSlot)) } answers { firstArg() }

        val out = service.cancelWaitlistSetupIntent(setupIntentId)

        assertThat(out).isNotNull()
        assertThat(out!!.status).isEqualTo(SetupStatus.CANCELED)
        assertThat(out.needsUserAction).isFalse()
        assertThat(out.clientSecret).isNull()
        verify(exactly = 1) { repo.save(any<WaitlistSetupState>()) }
    }

    @Test
    fun `getPaymentIntentClientSecret returns secret`() {
        val pi = mockk<PaymentIntent>()
        every { PaymentIntent.retrieve("pi_1") } returns pi
        every { pi.clientSecret } returns "sec_pi_1"

        val sec = service.getPaymentIntentClientSecret("pi_1")
        assertThat(sec).isEqualTo("sec_pi_1")
    }

    @Test
    fun `getPaymentIntentClientSecret throws when missing`() {
        val pi = mockk<PaymentIntent>()
        every { PaymentIntent.retrieve("pi_2") } returns pi
        every { pi.clientSecret } returns null

        assertThatThrownBy { service.getPaymentIntentClientSecret("pi_2") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("no client_secret")
    }

    @Test
    fun `cancelSetupIntent happy path`() {
        val setupIntentId = "seti_generic"
        val si = mkSetupIntent(setupIntentId, "requires_action")
        val canceled = mkSetupIntent(setupIntentId, "canceled")

        every { SetupIntent.retrieve(setupIntentId) } returns si
        every { si.cancel() } returns canceled

        val res = service.cancelSetupIntent(setupIntentId)

        assertThat(res.ok).isTrue()
        assertThat(res.intentId).isEqualTo(setupIntentId)
        assertThat(res.status).isEqualTo("canceled")
    }
}
