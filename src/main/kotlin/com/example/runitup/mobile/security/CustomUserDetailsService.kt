package com.example.runitup.mobile.security


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.service.TextService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val cacheManager: MyCacheManager
) : UserDetailsService {

    @Autowired
    lateinit var textService: TextService

    override fun loadUserByUsername(userId:String): UserDetails {
        print("loadUserByUsername userId = $userId")
        val user = cacheManager.getUser(userId)
        if(user == null){
            print("user not found, authentication failed")
            throw UsernameNotFoundException(textService.getText("user_not_found", LocaleContextHolder.getLocale().toString()))
        }
        if(!user.isActive){
            throw ApiRequestException(textService.getText("inactive_user", LocaleContextHolder.getLocale().toString()))
        }
        return UserPrincipal(user)

    }
}