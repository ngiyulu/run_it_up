package com.example.runitup.mobile.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Component
class JwtTokenService {

    @Value("\${security.jwt.secret-key}")
    var secret: String = ""

    @Value("\${security.jwt.access-ttl-months:2}")
    var accessTtlMonths: Long = 0

    private val key: Key by lazy {
        // HS256 requires a 256-bit key (>= 32 bytes)
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }


    fun generateToken(userDetails: UserDetails): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val expiry = now.plusMonths(accessTtlMonths)
        return Jwts.builder()
            .setSubject((userDetails as UserPrincipal).id)
            .setIssuedAt(Date.from(now.toInstant()))
            .setExpiration(Date.from(expiry.toInstant())) // <-- 2 months by default
            .signWith(key)
            .compact()
    }

    // inside JwtTokenService
    fun generateTokenWithExpiry(userDetails: UserDetails, expiry: ZonedDateTime): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        return Jwts.builder()
            .setSubject((userDetails as UserPrincipal).id)
            .setIssuedAt(Date.from(now.toInstant()))
            .setExpiration(Date.from(expiry.toInstant()))
            .signWith(key)
            .compact()
    }

    // convenience: expired token
    fun generateExpiredToken(userDetails: UserDetails, secondsInPast: Long = 60): String {
        val expiry = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(secondsInPast)
        return generateTokenWithExpiry(userDetails, expiry)
    }

    fun getUserIdFromJWT(token: String): String {
        return Jwts.parserBuilder().setSigningKey(key).build()
            .parseClaimsJws(token).body.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            false
        }
    }
}