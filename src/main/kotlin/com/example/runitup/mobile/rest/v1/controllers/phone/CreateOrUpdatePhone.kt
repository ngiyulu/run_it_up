package com.example.runitup.mobile.rest.v1.controllers.phone

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateOrUpdatePhone: BaseController<CreateOrUpdatePhone.CreateOrUpdatePhoneModel, Phone>() {

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: CreateOrUpdatePhoneModel): com.example.runitup.mobile.model.Phone {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
       return phoneService.createPhone(request.token, request.phoneOS, user.id.orEmpty())
    }

    class CreateOrUpdatePhoneModel(val token: FirebaseTokenModel, val phoneOS:String)
}