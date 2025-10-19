package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserDbRepositoryService: com.example.runitup.mobile.service.BaseService() {
    @Autowired
    lateinit var cacheManager: MyCacheManager


    fun deleteSessionFromUser(userId: String, sessionId: String){
        val user = cacheManager.getUser(userId) ?: throw ApiRequestException("user_not_found")
        user.apply {
            runSessions?.removeAll {
                it.id.toString() == sessionId || it.hostedBy == userId } }.let {
            cacheManager.updateUser(it) }

    }

}