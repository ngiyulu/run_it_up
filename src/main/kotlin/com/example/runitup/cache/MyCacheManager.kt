package com.example.runitup.cache

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class MyCacheManager {

    @Autowired
    lateinit var userRepository: UserRepository


    //user
    @Cacheable(CollectionConstants.USER_COLLECTION, key = "#id")
    fun getUser(id:String): User?{
        return userRepository.findByIdentifier(id)
    }


    @CachePut(CollectionConstants.USER_COLLECTION, key = "#user.identifier")
    fun updateUser(user:User):User{
        return userRepository.save(user)
    }

}