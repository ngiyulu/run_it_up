package com.example.runitup.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenProvider {
    private val jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512)
    private val jwtExpirationMs = 86400000

    fun generateToken(userDetails: UserDetails): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)
        return Jwts.builder()
            .setSubject((userDetails as UserPrincipal).id)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(jwtSecret)
            .compact()
    }

    fun getUserIdFromJWT(token: String): String {
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build()
            .parseClaimsJws(token).body.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            false
        }
    }
}