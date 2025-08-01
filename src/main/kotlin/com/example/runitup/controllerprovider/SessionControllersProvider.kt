package com.example.runitup.controllerprovider

import com.example.runitup.controller.runsession.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class SessionControllersProvider {

    @Autowired
    lateinit var confirmSessionController: ConfirmSessionController

    @Autowired
    lateinit var createSessionController: CreateSessionController

    @Autowired
    lateinit var getRunSessionController: GetRunSessionController

    @Autowired
    lateinit var joinSessionController: JoinSessionController

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
    lateinit var startSessionController: StartSessionController

    @Autowired
    lateinit var updateSessionGuest: UpdateSessionGuest
}