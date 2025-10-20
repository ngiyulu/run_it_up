package com.example.runitup.mobile.security.auth

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

class UserLoginAuthenticationToken(
    principal: String?,
    credentials: String?
) : UsernamePasswordAuthenticationToken(principal, credentials)