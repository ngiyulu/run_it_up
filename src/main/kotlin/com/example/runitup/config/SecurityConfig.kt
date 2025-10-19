package com.example.runitup.config

import com.example.runitup.handler.LoggingAccessDeniedHandler
import com.example.runitup.security.JwtAuthenticationFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig{

    @Autowired
    lateinit var userAuthenticationProvider: AuthenticationProvider

    @Autowired
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Autowired
    lateinit var accessDeniedHandler: LoggingAccessDeniedHandler

    @Autowired
    lateinit var jwtAuthEntryPoint: JwtAuthEntryPoint

    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    @Throws(java.lang.Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        log.info("securityFilterChain")
        http.csrf {
            it.disable()
        }
        http.anonymous().disable() // Disable anonymous authentication
        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }.authorizeHttpRequests {
            // require init and toke generrate to by passs authentication
            it.requestMatchers(
                "/api/v1/user/init",
                "/api/v1/user/token/generate/**",
                "/api/v1/user/verify",
                "/api/v1/user/create",
                "/api/v1/user/hello",
                "/api/v1/user/otp/verify",
                "/api/v1/user/logout",
                "/api/v1/user/otp/request",
                "/api/sms/send",
                "api/push/send",
                "api/queues/**",
                "api/test/cache/**",
                "api/sms/status/**",
                "/.well-known/apple-app-site-association", "/apple-app-site-association"
                ).permitAll()

            // everything else needs to be authenticated
            it.anyRequest().authenticated()
        }.exceptionHandling {
            it.authenticationEntryPoint(jwtAuthEntryPoint)       // 401 path
            it.accessDeniedHandler(accessDeniedHandler)    // 403 path
        }
//            .csrf { it.ignoringRequestMatchers("/.well-known/**", "/apple-app-site-association") }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Throws(java.lang.Exception::class)
    @Bean
    fun userAuthManager(http: HttpSecurity): AuthenticationManager {
        val authenticationManagerBuilder = http.getSharedObject(
            AuthenticationManagerBuilder::class.java
        )
        authenticationManagerBuilder.authenticationProvider(userAuthenticationProvider)
        return authenticationManagerBuilder.build()
    }
}