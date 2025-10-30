package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.support.CreateSupportController
import com.example.runitup.mobile.rest.v1.controllers.support.GetAllSupportController
import com.example.runitup.mobile.rest.v1.controllers.support.GetAllSupportModel
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/support")
@RestController
class SupportRestController {

    @Autowired
    lateinit var createSupport: CreateSupportController

    @PostMapping("/create")
    fun create(@RequestBody model: CreateSupportRequest): Support {
        return createSupport.execute(model)
    }



}