package com.example.runitup.controller

import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.JwtTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenService,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/register")
    fun register(@RequestBody user: User): ResponseEntity<Any> {
        userRepository.save(user)
        return ResponseEntity.ok("User registered")
    }

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: Map<String, String>): ResponseEntity<Map<String, String>> {
        val auth = UsernamePasswordAuthenticationToken(
            loginRequest["email"], loginRequest["password"]
        )
        val authentication = authenticationManager.authenticate(auth)
        val token = jwtTokenProvider.generateToken(authentication.principal as org.springframework.security.core.userdetails.UserDetails)
        return ResponseEntity.ok(mapOf("token" to token))
    }
}