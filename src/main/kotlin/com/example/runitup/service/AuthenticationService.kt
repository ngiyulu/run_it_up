package com.example.runitup.service

import com.example.runitup.cache.MyCacheManager
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthenticationService: BaseService() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var cacheManager: MyCacheManager

    fun createUser(user: User, locale:String): User? {
        val existingUser = cacheManager.getUser(user.id.toString())
        return userRepository.save(user)
    }

}