package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.repository.PhoneRepository
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import constant.HeaderConstants
import org.springframework.stereotype.Service

@Service
class PhoneService(private var phoneRepository: PhoneRepository): BaseService() {

    fun createPhone(token: FirebaseTokenModel, os:String, userId: String): Phone {
        var ph = phoneRepository.findByPhoneId(token.phoneId)
        var phoneType = PhoneType.ANDROID
        println("createPhone userId = $userId")
        if(ph == null){
            if(token.type == HeaderConstants.IOS_TYPE){
                phoneType = PhoneType.IOS
            }
            print("we didn't find phone")
            ph =phoneRepository.save(
                Phone(
                    os = os,
                    model = token.model.orEmpty(),
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
            if(token.type == HeaderConstants.IOS_TYPE){
                phoneType = PhoneType.IOS
            }
            if(ph.token != token.token || ph.userId != userId){
                ph.token = token.token
                ph.phoneId = token.phoneId
                ph.userId = userId
                ph.type = phoneType
                ph.model = token.model.orEmpty()
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

    fun getListOfPhone(userIds:List<String>): List<Phone>{
        return phoneRepository.findAllByUserIdIn(userIds)
    }

    fun getPhonesByUser(userId:String): List<Phone>{
        return phoneRepository.findAllByUserId(userId)
    }

}