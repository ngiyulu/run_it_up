package com.example.runitup.controllerprovider

import com.example.runitup.controller.InitializeController
import com.example.runitup.controller.user.*
import com.example.runitup.controller.user.update.UpdateDobController
import com.example.runitup.controller.user.update.UpdateEmailController
import com.example.runitup.controller.user.update.UpdateNameController
import com.example.runitup.controller.user.update.UpdateSkillLevelController
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
    lateinit var updateEmail: UpdateEmailController

    @Autowired
    lateinit var initializeController: InitializeController

    @Autowired
    lateinit var generateTokenController: GenerateTokenController

}