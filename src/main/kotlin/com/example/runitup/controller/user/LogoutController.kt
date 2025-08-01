package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.LogoutRequest
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.PhoneService
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