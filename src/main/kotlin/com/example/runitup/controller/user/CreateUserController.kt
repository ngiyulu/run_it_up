package com.example.runitup.controller.user

import com.example.runitup.constants.AppConstant
import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CreateUserRequest
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.AuthenticationService
import com.example.runitup.service.PasswordValidator
import com.example.runitup.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateUserController: BaseController<CreateUserRequest, User>() {
    @Autowired
    lateinit var authenticationService: AuthenticationService

    @Autowired
    lateinit var passwordValidator: PasswordValidator

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: CreateUserRequest): User {
        val existingUser = userRepository.findByAuth(request.user.auth)
        if(existingUser != null){
            throw  ApiRequestException(text("user_exist"))
        }
//        if(!passwordValidator.isValid(request.user.auth)){
//            throw  ApiRequestException(text("invalid_password"))
//        }
        val user = request.user
        user.verifiedPhone = false
        user.defaultPayment = AppConstant.WALLET
        val stripeId = paymentService.createCustomer(user) ?: throw ApiRequestException(text("stripe_error"))
        user.stripeId = stripeId
        return cacheManager.updateUser(user)
    }

}