package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GenerateTokenController: BaseController<String, GenerateTokenController.Token>() {

    @Autowired
    lateinit var jwtService: JwtTokenService
    override fun execute(request: String): Token {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        return Token(jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth)))
    }

    class Token(val token: String)
}