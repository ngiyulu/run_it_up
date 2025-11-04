package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CreateSupportController: BaseController<CreateSupportRequest, Support>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    override fun execute(request: CreateSupportRequest): Support {
        val s = Support(request.name, request.email, request.description).apply {
            createdAt = Instant.now()
        }
        return supportRepository.save(s)
    }

}