package com.example.runitup.mobile.security


import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.service.TextService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Autowired
    lateinit var textService: TextService

    override fun loadUserByUsername(userId:String): UserDetails {
        print("loadUserByUsername userId = $userId")
        val userDb = userRepository.findById(userId)
            if(!userDb.isPresent) {
                print("user not found, authentication failed")
                throw UsernameNotFoundException(textService.getText("user_not_found", LocaleContextHolder.getLocale().toString()))
            }
        val user = userDb.get()
        return UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth)
    }
}