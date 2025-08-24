package com.example.runitup.web.rest.v1.controllers.phone

import com.example.runitup.model.Phone
import com.example.runitup.service.PhoneService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.FirebaseTokenModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<FirebaseTokenModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: com.example.runitup.web.rest.v1.dto.FirebaseTokenModel): Phone {
       return phoneService.createPhone(request)
    }
}