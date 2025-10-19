package com.example.runitup.web.security


import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.service.TextService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomAdminDetailsService(
    private val adminUserRepository: AdminUserRepository
) : UserDetailsService {

    @Autowired
    lateinit var textService: TextService

    override fun loadUserByUsername(email:String): UserDetails {
        print("loadAdminByUsername email = $email")
        val admin = adminUserRepository.findByEmail(email) ?:   throw UsernameNotFoundException(textService.getText("admin_not_found", LocaleContextHolder.getLocale().toString()))
        return AdminPrincipal(admin)
    }
}