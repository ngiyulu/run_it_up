package com.example.runitup.restcontroller

import com.example.runitup.constants.HeaderConstants
import com.example.runitup.controllerprovider.UserControllersProvider
import com.example.runitup.dto.*
import com.example.runitup.dto.initialize.InitializeRequest
import com.example.runitup.dto.initialize.InitializeResponse
import com.example.runitup.dto.user.*
import com.example.runitup.model.Otp
import com.example.runitup.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/user")
@RestController
class UserRestController {

    @Autowired
    lateinit var userControllersProvider: UserControllersProvider

    @PostMapping("/verify")
    fun verifyUser(@RequestBody model: VerifyUserRequest,
                   @RequestHeader(HeaderConstants.TYPE)
                   type:String = HeaderConstants.ANDROID_TYPE): VerifyUserResponse? {
        model.firebaseTokenModel?.type = type
        return userControllersProvider.verifyUserController.execute(model)
    }

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello"
    }

    @PostMapping("/create")
    fun createUser(@RequestBody model: CreateUserRequest): User {
        return userControllersProvider.createUserController.execute(model)
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
    fun generateToken(@PathVariable id:String): String {
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
             @RequestHeader(HeaderConstants.TYPE)
             type:String = HeaderConstants.ANDROID_TYPE): InitializeResponse {
        model.firebaseTokenModel?.type = type
        return userControllersProvider.initializeController.execute(model)
    }

    @PostMapping("/update/skill")
    fun updateSkillLevel(@RequestBody model: UpdateSkillLevel): User {
        return userControllersProvider.updateSkillLevelController.execute(model)
    }

    @PostMapping("/update/dob")
    fun updateDob(@RequestBody model: UpdateDob): User {
        return userControllersProvider.updateDobController.execute(model)
    }

    @PostMapping("/otp/send")
    fun sendOtp(@RequestBody model: SendOtpRequest): OtpResponse {
        return userControllersProvider.sendOtpController.execute(model)
    }

    @GetMapping("/otp/get/{id}")
    fun getOtp(@PathVariable id:String): Otp {
        return userControllersProvider.getOtpController.execute(id)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody model: LogoutRequest) {
        return userControllersProvider.logOutController.execute(model)
    }
}