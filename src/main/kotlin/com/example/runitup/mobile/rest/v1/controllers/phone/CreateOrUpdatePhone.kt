package com.example.runitup.mobile.rest.v1.controllers.phone

import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import com.example.runitup.mobile.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<CreateOrUpdatePhone.CreateOrUpdatePhoneModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: CreateOrUpdatePhoneModel): com.example.runitup.mobile.model.Phone {
        val user =getMyUser()
       return phoneService.createPhone(request.token, request.phoneOS, user.id.orEmpty())
    }

    class CreateOrUpdatePhoneModel(val token: FirebaseTokenModel, val phoneOS:String)
}