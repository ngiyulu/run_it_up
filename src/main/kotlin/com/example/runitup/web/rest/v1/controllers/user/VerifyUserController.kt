package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.OtpService
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.VerifyUserRequest
import com.example.runitup.web.rest.v1.dto.VerifyUserResponse
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
        val user = userRepository.findByPhone(request.phone) ?: return VerifyUserResponse(null, null, "", "")
        request.firebaseTokenModel?.let {
            phoneService.createPhone(it)
        }
        otpService.createOtp(user)
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)
        }
       return VerifyUserResponse(null, null, user.id.orEmpty(), user.phoneNumber)
    }


}