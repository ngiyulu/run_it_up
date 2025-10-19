package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.LogoutRequest
import com.example.runitup.mobile.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LogoutController: BaseController<LogoutRequest, Unit>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    override fun execute(request: LogoutRequest){
        request.tokenModel?.let {
            phoneService.deletePhone(it)
        }
    }


}