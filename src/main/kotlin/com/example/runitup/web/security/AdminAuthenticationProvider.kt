package com.example.runitup.web.security

import com.example.runitup.web.security.auth.AdminLoginAuthenticationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AdminAuthenticationProvider : AuthenticationProvider {

    @Autowired
    lateinit var adminUserDetailsService: CustomAdminDetailsService

    @Autowired
    lateinit var  passwordEncoder: PasswordEncoder

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication? {
        println("AdminAuthenticationProvider")
        val rawPassword = authentication.credentials.toString()
        val email = authentication.principal as String
        val userDetails = adminUserDetailsService.loadUserByUsername(email)
        if (!passwordEncoder.matches(rawPassword, userDetails.password)) {
            throw BadCredentialsException("Invalid email or password")
        }
        return UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )
    }

    override fun supports(authentication: Class<*>): Boolean {
       return AdminLoginAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}