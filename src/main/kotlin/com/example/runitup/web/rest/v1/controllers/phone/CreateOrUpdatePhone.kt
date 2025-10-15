package com.example.runitup.web.rest.v1.controllers.phone

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Phone
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PhoneService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.FirebaseTokenModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<CreateOrUpdatePhone.CreateOrUpdatePhoneModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: CreateOrUpdatePhoneModel): Phone {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
       return phoneService.createPhone(request.token, request.phoneOS, user.id.orEmpty())
    }

    class CreateOrUpdatePhoneModel(val token: FirebaseTokenModel, val phoneOS:String)
}