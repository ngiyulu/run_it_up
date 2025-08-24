package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.constants.HeaderConstants
import com.example.runitup.model.Otp
import com.example.runitup.model.User
import com.example.runitup.web.rest.v1.controllerprovider.UserControllersProvider
import com.example.runitup.web.rest.v1.controllers.user.GenerateTokenController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/user")
@RestController
class UserRestController {

    @Autowired
    lateinit var userControllersProvider: UserControllersProvider

    @PostMapping("/verify")
    fun verifyUser(@RequestBody model: com.example.runitup.web.rest.v1.dto.VerifyUserRequest,
                   @RequestHeader(HeaderConstants.TYPE)
                   type:String = HeaderConstants.ANDROID_TYPE): com.example.runitup.web.rest.v1.dto.VerifyUserResponse? {
        model.firebaseTokenModel?.type = type
        return userControllersProvider.verifyUserController.execute(model)
    }

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello"
    }

    @PostMapping("/create")
    fun createUser(@RequestBody model: com.example.runitup.web.rest.v1.dto.CreateUserRequest): User {
        return userControllersProvider.createUserController.execute(model)
    }

    @PostMapping("/block")
    fun blockUser(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.BlockUser): User {
        return userControllersProvider.blockUserController.execute(model)
    }

    @GetMapping("/retrieve/{id}")
    fun getUser(@PathVariable id:String): User {
        return userControllersProvider.getUserController.execute(id)
    }

    @GetMapping("/token/generate/{id}")
    fun generateToken(@PathVariable id:String): GenerateTokenController.Token {
        return userControllersProvider.generateTokenController.execute(id)
    }


    @PostMapping("/update/name")
    fun updateName(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.UpdateNameModel): User {
        return userControllersProvider.updateNameController.execute(model)
    }

    @PostMapping("/update/email")
    fun updateEmail(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.UpdateEmailModel): User {
        return userControllersProvider.updateEmail.execute(model)
    }

    @PostMapping("/init")
    fun init(@RequestBody model: com.example.runitup.web.rest.v1.dto.initialize.InitializeRequest,
             @RequestHeader(HeaderConstants.TYPE)
             type:String = HeaderConstants.ANDROID_TYPE): com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse {
        model.firebaseTokenModel?.type = type
        return userControllersProvider.initializeController.execute(model)
    }

    @PostMapping("/update/skill")
    fun updateSkillLevel(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.UpdateSkillLevel): User {
        return userControllersProvider.updateSkillLevelController.execute(model)
    }

    @PostMapping("/update/sex")
    fun updateSkillLevel(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.UpdateSex): User {
        return userControllersProvider.updateSexController.execute(model)
    }

    @PostMapping("/update/dob")
    fun updateDob(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.UpdateDob): User {
        return userControllersProvider.updateDobController.execute(model)
    }

    @PostMapping("/otp/send")
    fun sendOtp(@RequestBody model: com.example.runitup.web.rest.v1.dto.SendOtpRequest): com.example.runitup.web.rest.v1.dto.OtpResponse {
        return userControllersProvider.sendOtpController.execute(model)
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(@RequestBody model: com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberRequest): com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberResponse {
        return userControllersProvider.verifyPhoneNumController.execute(model)
    }

    @GetMapping("/otp/get/{id}")
    fun getOtp(@PathVariable id:String): Otp {
        return userControllersProvider.getOtpController.execute(id)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody model: com.example.runitup.web.rest.v1.dto.LogoutRequest) {
        return userControllersProvider.logOutController.execute(model)
    }


}