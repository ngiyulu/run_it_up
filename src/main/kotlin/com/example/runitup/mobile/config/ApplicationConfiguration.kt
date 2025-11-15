package com.example.runitup.mobile.config

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.clicksend.ClickSendProperties
import com.example.runitup.mobile.constants.ConfigConstant
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.security.auth.UserAuthenticationProvider
import com.example.runitup.web.security.AdminAuthenticationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.*

@Configuration
@EnableConfigurationProperties(ClickSendProperties::class)
class ApplicationConfiguration {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var cache: MyCacheManager


    @Value("\${locale}")
    var locale: String = "es"

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder(11)
    }

    @Bean
    fun localeResolver(): LocaleResolver {
        val localeResolver = AcceptHeaderLocaleResolver()
        val locale = when(locale){
            "fr"-> Locale.FRENCH
            else -> Locale.ENGLISH
        }
        localeResolver.setDefaultLocale(locale)
        return localeResolver
    }

    @Bean
    @Qualifier(ConfigConstant.authenticationProvider)
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username: String ->
            println("userDetailsService: $username")
            val user = cache.getUser(username) ?: throw UsernameNotFoundException("User not found")
            UserPrincipal(user)
        }
    }

    @Bean
    @Qualifier(ConfigConstant.adminUserDetailsService)
    fun adminUserDetailsService(): UserDetailsService {
        return UserDetailsService { username: String ->
            println("userDetailsService: $username")
            val user = cache.getUser(username) ?: throw UsernameNotFoundException("User not found")
            UserPrincipal(user)
        }
    }



//    @Bean
//    fun getFireStoreClient():Firestore{
//        return FirestoreClient.getFirestore();
//    }

    @Bean
    @Qualifier(ConfigConstant.authenticationProvider)
    fun authProvider(): AuthenticationProvider {
        return UserAuthenticationProvider()
    }

    @Bean
    @Qualifier(ConfigConstant.adminAuthenticationProvider)
    fun adminAuthProvider(): AuthenticationProvider {
        return AdminAuthenticationProvider()
    }



}