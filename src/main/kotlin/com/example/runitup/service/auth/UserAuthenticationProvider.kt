package com.example.runitup.service.auth

import com.example.runitup.cache.MyCacheManager
import com.example.runitup.exception.ApiRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
        return UsernamePasswordAuthenticationToken(user, null)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}