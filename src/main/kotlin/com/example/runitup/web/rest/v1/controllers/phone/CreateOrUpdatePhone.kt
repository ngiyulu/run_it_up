package com.example.runitup.web.rest.v1.controllers.phone

import com.example.runitup.model.Phone
import com.example.runitup.service.PhoneService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.FirebaseTokenModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<CreateOrUpdatePhone.CreateOrUpdatePhoneModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: CreateOrUpdatePhoneModel): Phone {
       return phoneService.createPhone(request.token, request.phoneOS)
    }

    class CreateOrUpdatePhoneModel(val token: FirebaseTokenModel, val phoneOS:String)
}