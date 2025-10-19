package com.example.runitup.mobile.cache


import com.example.runitup.common.model.AdminUser
import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class MyCacheManager {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository


    //user
    @Cacheable(value = [CollectionConstants.USER_COLLECTION], key = "#id")
    fun getUser(id:String): User?{
        return userRepository.findByIdentifier(id)
    }


    @CachePut(value = [CollectionConstants.USER_COLLECTION], key = "#user.id")
    fun updateUser(user:User): User{
        return userRepository.save(user)
    }


    @Cacheable(value = [CollectionConstants.ADMIN_COLLECTION], key = "#id")
    fun getAdmin(id:String): AdminUser?{
        return adminUserRepository.findByIdentifier(id)
    }


    @CachePut(value = [CollectionConstants.ADMIN_COLLECTION], key = "#user.id")
    fun updateAdmin(admin:AdminUser):AdminUser{
        return adminUserRepository.save(admin)
    }

}