package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.myLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetSupportController: BaseController<String, Support>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    private val logger = myLogger()
    override fun execute(request: String): Support {
        val dbSupport = supportRepository.findById(request)
        if(!dbSupport.isPresent){
            throw  ApiRequestException("support not there")
        }
        val support = dbSupport.get()
        if(support.resolvedBy != null){
            support.admin = cacheManager.getAdmin(support.resolvedBy.orEmpty())
        }
        return  support
    }

}
