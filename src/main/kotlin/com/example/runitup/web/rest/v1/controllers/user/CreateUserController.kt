package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.constants.AppConstant
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.AuthenticationService
import com.example.runitup.service.PasswordValidator
import com.example.runitup.service.PaymentService
import com.example.runitup.service.http.MessagingService
import com.example.runitup.utility.AgeUtil
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.CreateUserRequest
import model.messaging.MessagingUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateUserController: BaseController<Pair<String, CreateUserRequest>, User>() {
    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: Pair<String, CreateUserRequest>): User {
        val  (zoneId, userRequest)= request
        val existingUser = userRepository.findByAuth(userRequest.user.phoneNumber)
        if(existingUser != null){
            throw  ApiRequestException(text("user_exist"))
        }
//        if(!passwordValidator.isValid(request.user.auth)){
//            throw  ApiRequestException(text("invalid_password"))
//        }
        val user = userRequest.user
        user.email = user.email.lowercase()
        user.verifiedPhone = false
        user.defaultPayment = AppConstant.WALLET
        val age = AgeUtil.ageFrom(user.dob, zoneIdString = zoneId)
        println("age = $age")
        if(!user.waiverSigned){
            user.waiverSigned = age >= 18
        }
        val stripeId = paymentService.createCustomer(user) ?: throw ApiRequestException(text("stripe_error"))
        user.stripeId = stripeId
        val newUser = cacheManager.updateUser(user)
        messagingService.createUser(
            MessagingUser(
            id = newUser.id,
            firstName = newUser.firstName,
            lastName = newUser.lastName,
            dob = newUser.dob,
            email = newUser.email,
            loggedInAt = newUser.loggedInAt,
            phoneNumber = newUser.phoneNumber,
            stripeId = newUser.stripeId,
            sex = newUser.sex,
            createdAt = newUser.createdAt,
            lastSeenAt = null,
            imageUrl = newUser.imageUrl

        )
        ).block()
        return newUser
    }

}