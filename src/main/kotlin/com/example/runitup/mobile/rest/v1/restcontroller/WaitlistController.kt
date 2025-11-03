package com.example.runitup.mobile.rest.v1.restcontroller
import com.example.runitup.mobile.rest.v1.controllers.waitlist.RefreshWaitListController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/waitlist")
class WaitlistController{

    @Autowired lateinit var refreshWaitListController: RefreshWaitListController

    @PostMapping("/refresh-setup")
    fun refreshSetup(
        @RequestBody req: WaitlistRefreshRequest
    ): WaitlistRefreshResponse {
        return refreshWaitListController.execute(req)
    }
}

data class WaitlistRefreshRequest(
    val setupIntentId: String,
    val bookingId: String,        // sanity check & linkage
    val paymentMethodId: String,  // optional but nice for validation
)

// responses/WaitlistRefreshResponse.kt
data class WaitlistRefreshResponse(
    val ok: Boolean,
    val status: String,               // "SUCCEEDED" | "REQUIRES_ACTION" | "CANCELED" | "ERROR"
    val setupIntentId: String,
    val needsUserAction: Boolean,
    val message: String? = null,
    val canAutoCharge: Boolean = false // your own boolean if you decide to attempt charge next
)