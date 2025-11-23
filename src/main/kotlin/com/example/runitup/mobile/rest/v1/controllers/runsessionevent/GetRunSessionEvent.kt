package com.example.runitup.mobile.rest.v1.controllers.runsessionevent

import com.example.runitup.mobile.repository.RunSessionEventRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetRunSessionEvent: BaseController<String, List<RunSessionEvent>>() {

    @Autowired
    lateinit var runSessionEventRepository: RunSessionEventRepository
    override fun execute(request: String): List<RunSessionEvent> {
       return runSessionEventRepository.findBySessionIdOrderByTsAsc(request)
    }

}