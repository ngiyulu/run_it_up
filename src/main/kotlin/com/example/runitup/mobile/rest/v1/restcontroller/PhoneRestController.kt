package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.rest.v1.controllers.phone.CreateOrUpdatePhone
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import constant.HeaderConstants
import constant.HeaderConstants.ANDROID_TYPE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/phone")
@RestController
class PhoneRestController {

    @Autowired
    lateinit var createOrUpdatePhone: CreateOrUpdatePhone


    @PostMapping("/create")
    fun create(@RequestBody model: FirebaseTokenModel,
               @RequestHeader(HeaderConstants.TYPE)  type:String = ANDROID_TYPE,
               @RequestHeader("X-OS-Version", required = true) phoneOs: String): com.example.runitup.mobile.model.Phone {
        model.type = type
        return createOrUpdatePhone.execute(CreateOrUpdatePhone.CreateOrUpdatePhoneModel(model, phoneOs))
    }
}