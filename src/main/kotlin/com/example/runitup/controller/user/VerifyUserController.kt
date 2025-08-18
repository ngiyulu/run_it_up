package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.VerifyUserRequest
import com.example.runitup.dto.VerifyUserResponse
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.OtpService
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyUserController: BaseController<VerifyUserRequest, VerifyUserResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var otpService: OtpService

    override fun execute(request: VerifyUserRequest): VerifyUserResponse? {
        val user = userRepository.findByPhone(request.phone) ?: throw  ApiRequestException("user_not_found")
        request.firebaseTokenModel?.let {
            phoneService.createPhone(it)
        }
        otpService.createOtp(user)
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)?.let { it ->
                it.map {
                    it.mapToUserPayment()
                }
            }
        }
       return VerifyUserResponse(null, null, user.id.orEmpty(), user.phoneNumber)
    }


}