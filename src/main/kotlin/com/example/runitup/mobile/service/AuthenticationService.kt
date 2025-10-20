package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthenticationService: com.example.runitup.mobile.service.BaseService() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var cacheManager: MyCacheManager

    fun createUser(user: User, locale:String): User? {
        val existingUser = cacheManager.getUser(user.id.toString())
        return userRepository.save(user)
    }

}