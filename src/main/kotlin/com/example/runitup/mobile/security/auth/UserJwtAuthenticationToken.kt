package com.example.runitup.mobile.security.auth

import com.example.runitup.mobile.model.User
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * Represents an authentication request or for an authenticated admin user.
 * Typically created by your JwtAuthenticationFilter when the incoming request path starts with /admin/
 */
class UserJwtAuthenticationToken(
    private val jwtToken: String?,
    private val user: User?,
    authorities: Collection<GrantedAuthority>? = emptyList()
) : AbstractAuthenticationToken(authorities) {

    override fun getCredentials(): Any? = jwtToken
    override fun getPrincipal(): Any? = user

    override fun isAuthenticated(): Boolean {
        // When authorities are non-empty, consider it authenticated
        return super.isAuthenticated()
    }

    companion object {
        /** Factory method for an authenticated user token. */
        fun authenticated(principal: User, authorities: Collection<GrantedAuthority>): UserJwtAuthenticationToken {
            val token = UserJwtAuthenticationToken(null, principal, authorities)
            token.isAuthenticated = true
            return token
        }

        /** Factory method for an unauthenticated user token (raw JWT before validation). */
        fun unauthenticated(jwtToken: String): UserJwtAuthenticationToken {
            val token = UserJwtAuthenticationToken(jwtToken, null, emptyList())
            token.isAuthenticated = false
            return token
        }
    }
}
