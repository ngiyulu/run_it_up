package com.example.runitup.service

import com.example.runitup.constants.HeaderConstants
import com.example.runitup.dto.FirebaseTokenModel
import com.example.runitup.enum.PhoneType
import com.example.runitup.model.Phone
import com.example.runitup.repository.PhoneRepository
import org.springframework.stereotype.Service

@Service
class PhoneService: BaseService() {

    lateinit var phoneRepository: PhoneRepository
    fun createPhone(token: FirebaseTokenModel): Phone{
        var ph = phoneRepository.findByPhoneId(token.phoneId)
        var phoneType = PhoneType.ANDROID
        if(ph == null){
            if(token.type == HeaderConstants.IOS_TYPE){
                phoneType = PhoneType.IOS
            }
            ph =phoneRepository.save(
                Phone(os = "",
                    model = "",
                    token = token.token,
                    phoneId = token.phoneId,
                    type = phoneType
                ))
        }
        else{
            ph.token = token.token
            ph.phoneId = token.phoneId
            phoneRepository.save(ph)
        }
        return ph
    }

    fun deletePhone(token: FirebaseTokenModel){
        val ph = phoneRepository.findByPhoneId(token.phoneId)
        if(ph != null){
            phoneRepository.delete(ph)
        }
    }

    fun getPhonesList():List<Phone> {
        return listOf()
    }
}