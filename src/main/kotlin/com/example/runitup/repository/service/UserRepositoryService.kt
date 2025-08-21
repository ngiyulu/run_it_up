package com.example.runitup.repository.service

import com.example.runitup.cache.MyCacheManager
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserRepositoryService: BaseService() {
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