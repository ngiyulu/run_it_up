package com.example.runitup.config

import com.example.runitup.cache.MyCacheManager
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.auth.UserAuthenticationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username: String ->
            println("userDetailsService: $username")
            val user = cache.getUser(username) ?: throw UsernameNotFoundException("User not found")
            UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber,user.auth,)
        }
    }

//    @Bean
//    fun getFireStoreClient():Firestore{
//        return FirestoreClient.getFirestore();
//    }

    @Bean
    @Qualifier("authenticationProvider")
    fun authenticationProvider(): AuthenticationProvider {
        return UserAuthenticationProvider()
    }



}