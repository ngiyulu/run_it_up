package com.example.runitup.web.rest.v2.controllerprovider

import com.example.runitup.web.rest.v2.controllers.runsession.GetMyBookingListV2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SessionControllersV2Provider {
    @Autowired
    lateinit var getMyBookingSessionController: GetMyBookingListV2
}