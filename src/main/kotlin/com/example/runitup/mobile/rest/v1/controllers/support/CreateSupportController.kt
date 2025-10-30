package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateSupportController: BaseController<CreateSupportRequest, Support>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    override fun execute(request: CreateSupportRequest): Support {
        return supportRepository.save(Support(request.name, request.email, request.description))
    }

}