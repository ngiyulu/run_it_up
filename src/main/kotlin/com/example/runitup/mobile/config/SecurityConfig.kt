package com.example.runitup.mobile.config

import com.example.runitup.mobile.constants.ConfigConstant
import com.example.runitup.mobile.handler.LoggingAccessDeniedHandler
import com.example.runitup.mobile.security.JwtAuthenticationFilter
import com.example.runitup.web.security.AdminJwtAuthenticationFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
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
    @Qualifier(ConfigConstant.authenticationProvider)
    lateinit var userAuthenticationProvider: AuthenticationProvider

    @Autowired
    @Qualifier(ConfigConstant.adminAuthenticationProvider)
    lateinit var adminAuthenticationProvider: AuthenticationProvider

    @Autowired
    lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Autowired
    lateinit var adminJwtAuthenticationFilter: AdminJwtAuthenticationFilter

    @Autowired
    lateinit var accessDeniedHandler: LoggingAccessDeniedHandler

    @Autowired
    lateinit var jwtAuthEntryPoint: JwtAuthEntryPoint

    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)


    @Bean
    @Order(1)
    fun adminChain(http: HttpSecurity): SecurityFilterChain {
        log.info("adminChain")
        http
            .securityMatcher("/admin/**") // everything under /admin uses this chain
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/admin/auth/v1/login",
                    "/admin/auth/v1/create",
                    "/admin/auth/v1/validate",
                    "/admin/login",
                    "admin/dashboard",
                    "admin/users/view/**",
                    "/admin/gyms",
                    "/admin/auth/login",
                    "admin/users",
                    "admin/waiver",
                    "/admin/runsessions",
                    "/admin/gyms/create",
                    "/admin/runsessions/create",
                    "admin/runsessions/edit/**",
                    "/admin/gyms/edit/**",
                ).permitAll()
                // everything else needs to be authenticated
                it.anyRequest().authenticated()
            }

            .exceptionHandling {
                it.authenticationEntryPoint(jwtAuthEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(adminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    @Order(2)
    @Throws(java.lang.Exception::class)
    fun userChain(http: HttpSecurity): SecurityFilterChain {
        log.info("userChain")
        http.csrf {
            it.disable()
        } .securityMatcher("api/**", "public/**") // all non-admin app endpoints
        http.anonymous().disable() // Disable anonymous authentication
        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }.authorizeHttpRequests {
            // require init and toke generrate to by passs authentication
            it.requestMatchers(
                "/api/v1/user/init",
                "api/v1/support/create",
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


    @Bean
    @Order(3)
    fun staticChain(http: HttpSecurity): SecurityFilterChain {
        // Optional: serve static assets & root/index without auth
        http
            .securityMatcher("/", "/index.html", "/styles.css", "/app.js", "/swagger-ui/**", "/v3/api-docs/**")
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
        return http.build()
    }

    @Bean
    @Throws(java.lang.Exception::class)
    fun authManager(http: HttpSecurity): AuthenticationManager {
        val authenticationManagerBuilder = http.getSharedObject(
            AuthenticationManagerBuilder::class.java
        )
        authenticationManagerBuilder.authenticationProvider(userAuthenticationProvider)
        authenticationManagerBuilder.authenticationProvider(adminAuthenticationProvider)
        return authenticationManagerBuilder.build()
    }

}