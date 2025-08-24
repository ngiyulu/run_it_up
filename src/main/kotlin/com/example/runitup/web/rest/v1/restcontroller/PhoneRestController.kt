package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.constants.HeaderConstants
import com.example.runitup.constants.HeaderConstants.ANDROID_TYPE
import com.example.runitup.model.Phone
import com.example.runitup.web.rest.v1.controllers.phone.CreateOrUpdatePhone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/phone")
@RestController
class PhoneRestController {

    @Autowired
    lateinit var createOrUpdatePhone: CreateOrUpdatePhone

    @PostMapping("/create")
    fun create(@RequestBody model: com.example.runitup.web.rest.v1.dto.FirebaseTokenModel, @RequestHeader(HeaderConstants.TYPE)  type:String = ANDROID_TYPE): Phone {
        model.type = type
        return createOrUpdatePhone.execute(model)
    }
}