package com.example.runitup.controller.phone

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.FirebaseTokenModel
import com.example.runitup.model.Phone
import com.example.runitup.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<FirebaseTokenModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: FirebaseTokenModel): Phone {
       return phoneService.createPhone(request)
    }
}