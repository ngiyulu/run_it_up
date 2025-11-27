package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.WaiverRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class WaiverServiceTest {

    private val waiverRepository: WaiverRepository = mock()

    // WaiverService uses field injection, so we set the field manually in the test
    private val waiverService = WaiverService().apply {
        this.waiverRepository = this@WaiverServiceTest.waiverRepository
    }

    @Test
    fun `setWaiverData - minor without waiver sets waiverSigned and waiverAuthorized false`() {
        // Given: user under 18, no waiver in DB
        val user = User(
            id = "user-1",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com"
        ).apply {
            waiverSigned = true   // set to true to ensure it gets overridden
            waiverAuthorized = true
        }

        whenever(waiverRepository.findByUserId("user-1")).thenReturn(null)

        // When
        waiverService.setWaiverData(user, age = 17)

        // Then
        assertFalse(user.waiverSigned, "Minor without waiver should have waiverSigned = false")
        assertFalse(user.waiverAuthorized, "Minor without waiver should have waiverAuthorized = false")
        assertNull(user.waiver, "Waiver object should remain null when none is found")

        verify(waiverRepository).findByUserId("user-1")
        verifyNoMoreInteractions(waiverRepository)
    }

    @Test
    fun `setWaiverData - minor with existing waiver attaches waiver to user without forcing flags`() {
        // Given: user under 18, waiver exists
        val user = User(
            id = "user-2",
            firstName = "Jane",
            lastName = "Smith",
            email = "jane@example.com"
        ).apply {
            waiverSigned = false
            waiverAuthorized = false
        }

        val existingWaiver = Waiver(
            id = "waiver-1",
            userId = "user-2",
            url = "https://example.com/waiver.jpg",
            status = WaiverStatus.APPROVED
        )

        whenever(waiverRepository.findByUserId("user-2")).thenReturn(existingWaiver)

        // When
        waiverService.setWaiverData(user, age = 16)

        // Then
        assertEquals(existingWaiver, user.waiver, "Existing waiver should be attached to user")
        // Service does NOT modify flags for this branch; they remain whatever they were
        assertFalse(user.waiverSigned, "Service should not force waiverSigned in minor + existing waiver branch")
        assertFalse(user.waiverAuthorized, "Service should not force waiverAuthorized in minor + existing waiver branch")

        verify(waiverRepository).findByUserId("user-2")
        verifyNoMoreInteractions(waiverRepository)
    }

    @Test
    fun `setWaiverData - adult sets waiverSigned and waiverAuthorized true and does not query repository`() {
        // Given: adult user, flags initially false
        val user = User(
            id = "adult-1",
            firstName = "Adult",
            lastName = "User",
            email = "adult@example.com"
        ).apply {
            waiverSigned = false
            waiverAuthorized = false
        }

        // When (age exactly 18 to test boundary)
        waiverService.setWaiverData(user, age = 18)

        // Then
        assertTrue(user.waiverSigned, "Adult should have waiverSigned = true")
        assertTrue(user.waiverAuthorized, "Adult should have waiverAuthorized = true")
        // Waiver object remains unchanged (null) unless set elsewhere
        assertNull(user.waiver, "Adult should not have waiver automatically attached")

        // Repository should never be called for adults
        verify(waiverRepository, never()).findByUserId(any())
        verifyNoMoreInteractions(waiverRepository)
    }

    @Test
    fun `setWaiverData - minor with null user id uses empty string for repository lookup`() {
        // Given: user under 18 with null id
        val user = User(
            id = null,
            firstName = "NoId",
            lastName = "User",
            email = "noid@example.com"
        ).apply {
            waiverSigned = true
            waiverAuthorized = true
        }

        whenever(waiverRepository.findByUserId("")).thenReturn(null)

        // When
        waiverService.setWaiverData(user, age = 15)

        // Then
        assertFalse(user.waiverSigned, "Minor without waiver should have waiverSigned = false")
        assertFalse(user.waiverAuthorized, "Minor without waiver should have waiverAuthorized = false")
        assertNull(user.waiver)

        verify(waiverRepository).findByUserId("")
        verifyNoMoreInteractions(waiverRepository)
    }
}
