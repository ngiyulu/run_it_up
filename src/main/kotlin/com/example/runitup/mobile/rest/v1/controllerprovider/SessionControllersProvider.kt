package com.example.runitup.mobile.rest.v1.controllerprovider

import com.example.runitup.mobile.rest.v1.controllers.runsession.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class SessionControllersProvider {

    @Autowired
    lateinit var confirmSessionController: ConfirmSessionController

    @Autowired
    lateinit var getRunSessionController: GetRunSessionController

    @Autowired
    lateinit var runHistoryByDateController: RunHistoryByDateController

    @Autowired
    lateinit var getRunSessionListController: GetRunSessionListController

    @Autowired
    lateinit var getMyBookingSessionController: GetMyBookingList

    @Autowired
    lateinit var joinSessionController: JoinSessionController

    @Autowired
    lateinit var joinWaitListController: JoinWaitListController

    @Autowired
    lateinit var updateSessionController: UpdateSessionController

    @Autowired
    lateinit var cancelSessionController: CancelSessionController

    @Autowired
    lateinit var checkInController: CheckInController

    @Autowired
    lateinit var completeSessionController: CompleteSessionController

    @Autowired
    lateinit var leaveSessionController: LeaveSessionController

    @Autowired
    lateinit var cancelBookingController: CancelBookingController

    @Autowired
    lateinit var startSessionController: StartSessionController

    @Autowired
    lateinit var updateSessionGuest: UpdateSessionGuest

    @Autowired
    lateinit var createPaymentIntent: CreatePaymentIntent
}