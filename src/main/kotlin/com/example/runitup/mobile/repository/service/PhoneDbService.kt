package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.repository.PhoneRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PhoneDbService {
    @Autowired
    lateinit var phoneRepository: PhoneRepository

    fun findByPhoneId(phoneId: String): com.example.runitup.mobile.model.Phone?{
        return  phoneRepository.findByPhoneId(phoneId)
    }
    fun findAllByPhoneId(phoneId: String): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByPhoneId(phoneId)
    }


    fun findAllByType(type: PhoneType): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByType(type)
    }


    fun findAllByTokenIn(tokens: Collection<String>): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByTokenIn(tokens)
    }

    /**
     * Find all phones belonging to a list of IDs.
     */
    fun findAllByIdIn(ids: Collection<String>): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByTokenIn(ids)
    }

    fun findAllByUserId(userId: String): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByUserId(userId)
    }

    fun findAllByUserIdIn(userIds: Collection<String>): List<com.example.runitup.mobile.model.Phone>{
        return  phoneRepository.findAllByUserIdIn(userIds)
    }
}