package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.user.GenerateTokenController
import com.example.runitup.mobile.rest.v1.controllers.user.VerifyPhoneNumberController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeResponse
import com.example.runitup.mobile.rest.v1.dto.user.*
import constant.HeaderConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/user")
@RestController
class UserRestController {

    @Autowired
    lateinit var userControllersProvider: com.example.runitup.mobile.rest.v1.controllerprovider.UserControllersProvider



    @PostMapping("/verify")
    fun verifyUser(@RequestBody model: VerifyUserRequest,
                   @RequestHeader("X-Timezone", required = true) tzHeader: String): VerifyUserResponse? {
        return userControllersProvider.verifyUserController.execute(Pair(tzHeader, model))
    }

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello"
    }

    @PostMapping("/create")
    fun createUser(
        @RequestBody model: CreateUserRequest,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
    ): User {
        return userControllersProvider.createUserController.execute(Pair(tzHeader,model))
    }

    @PostMapping("/block")
    fun blockUser(@RequestBody model: BlockUser): User {
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
    fun updateName(@RequestBody model: UpdateNameModel): User {
        return userControllersProvider.updateNameController.execute(model)
    }

    @PostMapping("/update/email")
    fun updateEmail(@RequestBody model: UpdateEmailModel): User {
        return userControllersProvider.updateEmail.execute(model)
    }

    @PostMapping("/init")
    fun init(@RequestBody model: InitializeRequest,
             @RequestHeader(HeaderConstants.TYPE) type:String = HeaderConstants.ANDROID_TYPE,
             @RequestHeader("X-OS-Version", required = true) phoneOs: String): InitializeResponse {
        model.tokenModel?.type = type
        model.os = phoneOs
        return userControllersProvider.initializeController.execute(model)
    }

    @PostMapping("/update/skill")
    fun updateSkillLevel(@RequestBody model: UpdateSkillLevel): User {
        return userControllersProvider.updateSkillLevelController.execute(model)
    }

    @PostMapping("/update/sex")
    fun updateSkillLevel(@RequestBody model: UpdateSex): User {
        return userControllersProvider.updateSexController.execute(model)
    }

    @PostMapping("/update/dob")
    fun updateDob(@RequestBody model: UpdateDob): User {
        return userControllersProvider.updateDobController.execute(model)
    }

    @PostMapping("/otp/request")
    fun sendOtp(@RequestBody model: SendOtpRequest): OtpResponse {
        return userControllersProvider.requestOtpController.execute(model)
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(
        @RequestBody model: VerifyPhoneNumberRequest,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
        @RequestHeader("model") phoneModel: String = "",
        @RequestHeader("X-OS-Version", required = true) phoneOs: String,
        @RequestHeader(HeaderConstants.TYPE) type:String = HeaderConstants.ANDROID_TYPE): VerifyPhoneNumberResponse {
        model.tokenModel?.type = type

        return userControllersProvider.verifyPhoneNumController.execute(VerifyPhoneNumberController.VerifyPhoneNumberControllerModel(tzHeader, model, phoneOs, phoneModel))
    }

    @GetMapping("/otp/get/{id}")
    fun getOtp(@PathVariable id:String): com.example.runitup.mobile.model.Otp {
        return userControllersProvider.getOtpController.execute(id)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody model: LogoutRequest) {
        return userControllersProvider.logOutController.execute(model)
    }




}