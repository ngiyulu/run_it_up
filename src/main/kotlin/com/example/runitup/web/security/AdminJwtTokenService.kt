package com.example.runitup.web.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Component
class AdminJwtTokenService {

    @Value("\${security.jwt.secret-key}")
    var secret: String = ""

    @Value("\${security.jwt.access-ttl-months:2}")
    var accessTtlMonths: Long = 0

    private lateinit var jwtKey: Key

    @PostConstruct
    fun init() {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "security.jwt.secret-key must be at least 32 bytes for HS256"
        }
        jwtKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateToken(userDetails: UserDetails): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val expiry = now.plusMonths(accessTtlMonths)
        val principal = userDetails as AdminPrincipal
        return Jwts.builder()
            .setSubject("${principal.admin.id}:${principal.admin.email}")
            .setIssuedAt(Date.from(now.toInstant()))
            .setExpiration(Date.from(expiry.toInstant())) // <-- 2 months by default
            .signWith(jwtKey)
            .compact()
    }

    fun getAdminDataFromJwtToken(token: String): AdminJwtToken {
        val subject =  Jwts.parserBuilder().setSigningKey(jwtKey).build()
            .parseClaimsJws(token).body.subject
        val data = subject.split(":")
        return AdminJwtToken(data[0], data[1])
    }

    class AdminJwtToken(val userId:String, val email:String)


    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(jwtKey).build().parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            false
        }
    }
}