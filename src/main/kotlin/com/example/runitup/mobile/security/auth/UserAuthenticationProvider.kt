package com.example.runitup.mobile.security.auth

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component

@Component
class UserAuthenticationProvider : AuthenticationProvider {

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication? {
        println("CustomAuthenticationProvider")
        val user = cacheManager.getUser(authentication.name) ?: throw ApiRequestException("User not found")
        return UserJwtAuthenticationToken.authenticated(user, listOf())
    }

    override fun supports(authentication: Class<*>): Boolean {
        return UserLoginAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}