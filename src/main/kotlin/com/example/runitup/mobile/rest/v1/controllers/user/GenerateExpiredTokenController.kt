package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Token
import com.example.runitup.mobile.security.JwtTokenService
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GenerateExpiredTokenController: BaseController<String, Token>() {

    @Autowired
    lateinit var jwtService: JwtTokenService
    override fun execute(request: String): Token {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        return Token(jwtService.generateExpiredToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth)))
    }


}