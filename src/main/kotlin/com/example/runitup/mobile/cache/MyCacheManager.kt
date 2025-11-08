package com.example.runitup.mobile.cache


import com.example.runitup.common.model.AdminUser
import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class MyCacheManager (
    private var userRepository: UserRepository,
    private var adminUserRepository: AdminUserRepository,
    private var runSessionRepository: RunSessionRepository){


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

    @Cacheable(value = [CollectionConstants.SESSION_COLLECTION], key = "#id")
    fun getRunSession(id:String): RunSession?{
        return runSessionRepository.findByIdentifier(id)
    }

    @CachePut(value = [CollectionConstants.SESSION_COLLECTION], key = "#session.id")
    fun updateRunSession(session:RunSession): RunSession{
        return runSessionRepository.save(session)
    }

    /**
     * Explicitly evicts a session from the cache.
     * Useful when a run session is updated or deleted outside of repository methods.
     */
    @CacheEvict(value = [CollectionConstants.SESSION_COLLECTION], key = "#id")
    fun evictRunSession(id: String) {
        // Optionally, log or perform extra cleanup
        println("Evicted RunSession from cache: $id")
    }

}