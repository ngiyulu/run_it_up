package com.example.runitup.web.security.auth

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

// For username/password login requests
class AdminLoginAuthenticationToken(
    principal: String?,
    credentials: String?
) : UsernamePasswordAuthenticationToken(principal, credentials)