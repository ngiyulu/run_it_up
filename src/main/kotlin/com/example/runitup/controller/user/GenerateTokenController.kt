package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GenerateTokenController: BaseController<String, String>() {

    @Autowired
    lateinit var jwtService: JwtService
    override fun execute(request: String): String {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        return jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
    }
}