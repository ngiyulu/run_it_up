package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.utility.AgeUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class WaiverService {
    @Autowired
    lateinit var waiverRepository: WaiverRepository
    fun setWaiverData(user: User, age:Int){
        println("age = $age")
        if(age < 18){
            val waiver = waiverRepository.findByUserId(user.id.orEmpty())
            if(waiver == null){
                user.waiverSigned = false
                user.waiverAuthorized = false
            }
            else if(waiver.status == WaiverStatus.APPROVED){
                user.waiverSigned = true
                user.waiverAuthorized = true
            }
            else{
                user.waiverSigned = true
                user.waiverAuthorized = false
                user.rejectionNotes = waiver.note
            }
        }
        else{
            user.waiverSigned = true
            user.waiverAuthorized = true
        }
    }
}