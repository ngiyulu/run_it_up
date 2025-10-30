package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.rest.v1.controllers.support.CreateSupportController
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/public/support")
@RestController
class SupportRestController {

    @Autowired
    lateinit var createSupport: CreateSupportController
    @PostMapping("/create")
    fun create(@RequestBody model: CreateSupportRequest): Support {
        return createSupport.execute(model)
    }

}