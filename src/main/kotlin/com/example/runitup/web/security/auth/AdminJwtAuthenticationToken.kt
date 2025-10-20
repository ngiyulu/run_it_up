package com.example.runitup.web.security.auth

import com.example.runitup.common.model.AdminUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * Represents an authentication request or for an authenticated admin user.
 * Typically created by your JwtAuthenticationFilter when the incoming request path starts with /admin/
 */
class AdminJwtAuthenticationToken(
    private val jwtToken: String?,
    private val principalValue: AdminUser? = null,
    authorities: Collection<GrantedAuthority>? = emptyList()
) : AbstractAuthenticationToken(authorities) {

    override fun getCredentials(): Any? = jwtToken
    override fun getPrincipal(): Any? = principalValue

    override fun isAuthenticated(): Boolean {
        // When authorities are non-empty, consider it authenticated
        return super.isAuthenticated()
    }

    companion object {
        /** Factory method for an authenticated admin token. */
        fun authenticated(principal: AdminUser, authorities: Collection<GrantedAuthority>): AdminJwtAuthenticationToken {
            val token = AdminJwtAuthenticationToken(null, principal, authorities)
            token.isAuthenticated = true
            return token
        }

        /** Factory method for an unauthenticated admin token (raw JWT before validation). */
        fun unauthenticated(jwtToken: String): AdminJwtAuthenticationToken {
            val token = AdminJwtAuthenticationToken(jwtToken, null, emptyList())
            token.isAuthenticated = false
            return token
        }
    }
}
