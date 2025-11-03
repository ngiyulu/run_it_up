package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.WaitlistSetupState
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.WAILIST_STEUP_STATE_COLLECTION)
interface WaitlistSetupStateRepository : MongoRepository<WaitlistSetupState, String> {
    fun findBySetupIntentId(setupIntentId: String): WaitlistSetupState?
    fun findByBookingIdAndPaymentMethodId(bookingId: String, paymentMethodId: String): WaitlistSetupState?
}