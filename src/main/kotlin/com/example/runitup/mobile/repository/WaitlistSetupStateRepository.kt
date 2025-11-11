package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.SetupStatus
import com.example.runitup.mobile.model.WaitlistSetupState
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.WAILIST_STEUP_STATE_COLLECTION)
interface WaitlistSetupStateRepository : MongoRepository<WaitlistSetupState, String> {
    fun findBySetupIntentId(setupIntentId: String): WaitlistSetupState?
    fun findByBookingIdAndPaymentMethodId(bookingId: String, paymentMethodId: String): WaitlistSetupState?

    // ✅ Find all setup states for a session (e.g., cleanup or audit)
    fun findAllBySessionId(sessionId: String): List<WaitlistSetupState>

    // ✅ Find all pending or action-required setups for a user
    fun findAllByUserIdAndStatusIn(userId: String, status: Collection<SetupStatus>): List<WaitlistSetupState>

    // ✅ Find all that need user action
    fun findAllByNeedsUserActionTrue(): List<WaitlistSetupState>

    // ✅ Find recent setups (for logs or dashboards)
    fun findAllByCreatedAtBetween(start: Long, end: Long): List<WaitlistSetupState>
}
