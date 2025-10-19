package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.repository.PhoneRepository
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import constant.HeaderConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PhoneService: BaseService() {

    @Autowired
    lateinit var phoneRepository: PhoneRepository
    fun createPhone(token: FirebaseTokenModel, os:String, userId: String): com.example.runitup.mobile.model.Phone {
        var ph = phoneRepository.findByPhoneId(token.phoneId)
        var phoneType = PhoneType.ANDROID
        if(ph == null){
            if(token.type == HeaderConstants.IOS_TYPE){
                phoneType = PhoneType.IOS
            }
            print("we didn't find phone")
            ph =phoneRepository.save(
                com.example.runitup.mobile.model.Phone(
                    os = os,
                    model = "",
                    userId = userId,
                    token = token.token,
                    phoneId = token.phoneId,
                    type = phoneType
                )
            )
        }
        else{
            print("we found phone")
            print("old phone token = ${ph.token}")
            print("new phone token = ${token.token}")
            if(ph.token != token.token){
                ph.token = token.token
                ph.phoneId = token.phoneId
                print("update phone")
                phoneRepository.save(ph)
            }


        }
        return ph
    }

    fun deletePhone(token: FirebaseTokenModel){
        val ph = phoneRepository.findByPhoneId(token.phoneId)
        if(ph != null){
            phoneRepository.delete(ph)
        }
    }

    fun getPhonesList():List<com.example.runitup.mobile.model.Phone> {
        return listOf()
    }
}