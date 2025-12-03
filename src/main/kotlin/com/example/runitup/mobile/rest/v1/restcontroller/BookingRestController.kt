package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.rest.v1.controllers.booking.BookingDetails
import com.example.runitup.mobile.rest.v1.controllers.booking.GetBookingDetailsController
import com.example.runitup.mobile.rest.v1.controllers.booking.UpdateBookingPaymentController
import com.example.runitup.mobile.rest.v1.controllers.booking.UpdateBookingPaymentModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/booking")
@RestController
class BookingRestController {

    @Autowired
    lateinit var getBookingDetails: GetBookingDetailsController

    @Autowired
    lateinit var updateBookingPaymentController: UpdateBookingPaymentController


    @PostMapping("/payment/update")
    fun updatePayment(@RequestBody model: UpdateBookingPaymentModel): BookingDetails {
        return updateBookingPaymentController.execute(model)
    }

    @GetMapping("/details/{runSessionId}")
    fun getBookingDetails(@PathVariable runSessionId: String): BookingDetails {
        return getBookingDetails.execute(runSessionId)
    }
}
