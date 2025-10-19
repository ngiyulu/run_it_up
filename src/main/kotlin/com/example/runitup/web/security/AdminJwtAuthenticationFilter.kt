package com.example.runitup.web.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AdminJwtAuthenticationFilter(
    private val jwtTokenProvider: AdminJwtTokenService,
    private val customUserDetailsService: CustomAdminDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")
        println("token = $token")
        if (token != null && jwtTokenProvider.validateToken(token) && request.requestURI.contains("admin")) {
            val adminData = jwtTokenProvider.getAdminDataFromJwtToken(token)
            val userDetails = customUserDetailsService.loadUserByUsername(adminData.email)
            val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
        }
        filterChain.doFilter(request, response)
    }
}