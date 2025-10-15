package com.example.runitup.repository.service

import com.example.runitup.enum.PhoneType
import com.example.runitup.model.Phone
import com.example.runitup.repository.PhoneRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PhoneDbService {
    @Autowired
    lateinit var phoneRepository: PhoneRepository

    fun findByPhoneId(phoneId: String): Phone?{
        return  phoneRepository.findByPhoneId(phoneId)
    }
    fun findAllByPhoneId(phoneId: String): List<Phone>{
        return  phoneRepository.findAllByPhoneId(phoneId)
    }


    fun findAllByType(type: PhoneType): List<Phone>{
        return  phoneRepository.findAllByType(type)
    }


    fun findAllByTokenIn(tokens: Collection<String>): List<Phone>{
        return  phoneRepository.findAllByTokenIn(tokens)
    }

    /**
     * Find all phones belonging to a list of IDs.
     */
    fun findAllByIdIn(ids: Collection<String>): List<Phone>{
        return  phoneRepository.findAllByTokenIn(ids)
    }

    fun findAllByUserId(userId: String): List<Phone>{
        return  phoneRepository.findAllByUserId(userId)
    }
}