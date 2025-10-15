package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.model.Otp
import com.example.runitup.model.User
import com.example.runitup.web.rest.v1.controllerprovider.UserControllersProvider
import com.example.runitup.web.rest.v1.controllers.user.GenerateTokenController
import com.example.runitup.web.rest.v1.controllers.user.VerifyPhoneNumberController
import constant.HeaderConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/user")
@RestController
class UserRestController {

    @Autowired
    lateinit var userControllersProvider: UserControllersProvider

    @PostMapping("/verify")
    fun verifyUser(@RequestBody model: com.example.runitup.web.rest.v1.dto.VerifyUserRequest,
                   @RequestHeader("X-Timezone", required = true) tzHeader: String): com.example.runitup.web.rest.v1.dto.VerifyUserResponse? {
        return userControllersProvider.verifyUserController.execute(Pair(tzHeader, model))
    }

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello"
    }

    @PostMapping("/create")
    fun createUser(
        @RequestBody model: com.example.runitup.web.rest.v1.dto.CreateUserRequest,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
    ): User {
        return userControllersProvider.createUserController.execute(Pair(tzHeader,model))
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
             @RequestHeader(HeaderConstants.TYPE) type:String = HeaderConstants.ANDROID_TYPE,
             @RequestHeader("X-OS-Version", required = true) phoneOs: String): com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse {
        model.tokenModel?.type = type
        model.os = phoneOs
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

    @PostMapping("/otp/request")
    fun sendOtp(@RequestBody model: com.example.runitup.web.rest.v1.dto.SendOtpRequest): com.example.runitup.web.rest.v1.dto.OtpResponse {
        return userControllersProvider.requestOtpController.execute(model)
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(
        @RequestBody model: com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberRequest,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
        @RequestHeader("model") phoneModel: String = "",
        @RequestHeader("X-OS-Version", required = true) phoneOs: String,
        @RequestHeader(HeaderConstants.TYPE) type:String = HeaderConstants.ANDROID_TYPE): com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberResponse {
        model.tokenModel?.type = type

        return userControllersProvider.verifyPhoneNumController.execute(VerifyPhoneNumberController.VerifyPhoneNumberControllerModel(tzHeader, model, phoneOs, phoneModel))
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