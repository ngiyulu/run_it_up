package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.model.SupportStatus
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ResolveSupportTicketController: BaseController<ResolveSupportTicketModel, Support>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    override fun execute(request: ResolveSupportTicketModel): Support {
        val auth =  SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        val dbSupport = supportRepository.findById(request.supportTicketId)
        if(!dbSupport.isPresent){
            throw  ApiRequestException("support not there")
        }

        val support = dbSupport.get()
        if(support.status == SupportStatus.RESOLVED){
            return  support
        }
        support.notes =request.notes
        support.resolvedAt = LocalDate.now()
        support.resolvedBy = savedAdmin.admin.id
        support.status = SupportStatus.RESOLVED
        return  supportRepository.save(support)

    }

}

class ResolveSupportTicketModel(val supportTicketId:String, val notes:String)