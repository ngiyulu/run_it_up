package com.example.runitup.mobile.rest.v1.controllers.support

import com.example.runitup.mobile.enum.toEnumOrNull
import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.model.SupportStatus
import com.example.runitup.mobile.repository.SupportRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class GetAllSupportController: BaseController<GetAllSupportModel, List<Support>>() {

    @Autowired
    lateinit var supportRepository: SupportRepository
    override fun execute(request: GetAllSupportModel): List<Support> {
        var support= if(request.status == null) supportRepository.findAll()
        else if(request.date != null ) {
            val startUtc = request.date.atStartOfDay(ZoneOffset.UTC).toInstant()
            val endUtc = request.date.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
            supportRepository.findByStatusAndCreatedAtBetween(request.status.toEnumOrNull<SupportStatus>()!!, startUtc, endUtc!!)
        }
        else supportRepository.findByStatus(request.status.toEnumOrNull<SupportStatus>()!!)
        support = support.map {
            if(it.resolvedBy != null){
                it.admin = cacheManager.getAdmin(it.resolvedBy.orEmpty())
            }
            it
        }
        return  support

    }

}
class GetAllSupportModel(val pageRequest: PageRequest, val status:String?, val date:LocalDate?)