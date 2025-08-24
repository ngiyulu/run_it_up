package com.example.runitup.web.rest.v1.controllerprovider

import com.example.runitup.web.rest.v1.controllers.InitializeController
import com.example.runitup.web.rest.v1.controllers.user.*
import com.example.runitup.web.rest.v1.controllers.user.update.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserControllersProvider {

    @Autowired
    lateinit var getUserController: GetUserController

    @Autowired
    lateinit var verifyUserController: VerifyUserController

    @Autowired
    lateinit var createUserController: CreateUserController

    @Autowired
    lateinit var logOutController: LogoutController

    @Autowired
    lateinit var blockUserController: BlockUserController

    @Autowired
    lateinit var updateDobController: UpdateDobController

    @Autowired
    lateinit var updateNameController: UpdateNameController

    @Autowired
    lateinit var updateSkillLevelController: UpdateSkillLevelController

    @Autowired
    lateinit var updateSexController: UpdateSexController

    @Autowired
    lateinit var updateEmail: UpdateEmailController

    @Autowired
    lateinit var initializeController: InitializeController

    @Autowired
    lateinit var generateTokenController: GenerateTokenController

    @Autowired
    lateinit var sendOtpController: SendOtpController

    @Autowired
    lateinit var getOtpController: GetOtpController

    @Autowired
    lateinit var verifyPhoneNumController: VerifyPhoneNumberController
}