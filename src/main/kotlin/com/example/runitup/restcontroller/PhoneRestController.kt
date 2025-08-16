package com.example.runitup.restcontroller

import com.example.runitup.constants.HeaderConstants
import com.example.runitup.constants.HeaderConstants.ANDROID_TYPE
import com.example.runitup.controller.phone.CreateOrUpdatePhone
import com.example.runitup.dto.FirebaseTokenModel
import com.example.runitup.model.Phone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/phone")
@RestController
class PhoneRestController {

    @Autowired
    lateinit var createOrUpdatePhone: CreateOrUpdatePhone

    @PostMapping("/create")
    fun create(@RequestBody model: FirebaseTokenModel, @RequestHeader(HeaderConstants.TYPE)  type:String = ANDROID_TYPE): Phone {
        model.type = type
        return createOrUpdatePhone.execute(model)
    }
}