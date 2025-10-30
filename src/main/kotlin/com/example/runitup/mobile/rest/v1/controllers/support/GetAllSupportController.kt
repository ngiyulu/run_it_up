package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GetAllSupportController: BaseController<GetAllSupportModel, List<Support>>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    override fun execute(request: GetAllSupportModel): List<Support> {
        if(request.status == null){
            return  supportRepository.findAll()
        }
        return  supportRepository.findByStatus(request.status)
    }

}
class GetAllSupportModel(val pageRequest: PageRequest, val status:String?)