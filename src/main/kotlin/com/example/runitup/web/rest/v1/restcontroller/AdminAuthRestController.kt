package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.common.model.AdminUser
import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.web.config.AdminLoginResponse
import com.example.runitup.web.dto.AdminLoginRequest
import com.example.runitup.web.dto.CreateAdminRequest
import com.example.runitup.web.security.AdminJwtTokenService
import com.example.runitup.web.security.AdminPrincipal
import com.example.runitup.web.security.auth.AdminLoginAuthenticationToken
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/auth/v1")
class AdminAuthRestController{


    @Autowired
    lateinit var authenticationManager: AuthenticationManager

    @Autowired
    lateinit var jwtTokenProvider: AdminJwtTokenService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository
    @PostMapping("/login")
    fun login(@RequestBody body: AdminLoginRequest): AdminLoginResponse {
        val auth = authenticationManager.authenticate(
            AdminLoginAuthenticationToken(body.email, body.password)
        )
        val admin = auth.principal as AdminPrincipal
        return AdminLoginResponse (admin.admin, jwtTokenProvider.generateToken(admin))
    }

    @PostMapping("/create")
    fun create(@RequestBody body: CreateAdminRequest): AdminUser {
        val adminUser = AdminUser(
            ObjectId().toString(),
            body.email,
            body.firstName,
            body.lastName,
            passwordEncoder.encode(body.password))
        return adminUserRepository.save(adminUser)
    }

    @GetMapping("/validate")
    //validate if a token is expired from html page
    fun validate(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                .body(mapOf("valid" to false, "message" to "Missing or invalid Authorization header"))
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        return try {
            if (!jwtTokenProvider.validateToken(token)) {
                ResponseEntity.status(401)
                    .body(mapOf("valid" to false, "message" to "Token invalid or expired"))
            } else {
                val userId = jwtTokenProvider.getAdminDataFromJwtToken(token)
                ResponseEntity.ok(mapOf("valid" to true, "userId" to userId))
            }
        } catch (ex: Exception) {
            ResponseEntity.status(401)
                .body(mapOf("valid" to false, "message" to (ex.message ?: "Invalid token")))
        }
    }
}