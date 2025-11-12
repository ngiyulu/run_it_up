package com.example.runitup.web.rest.v1.restcontroller.dev

import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.web.security.AdminJwtTokenService
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/dev/")
@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles

class DevRestController{

    @Autowired
    lateinit var adminJwtTokenProvider: AdminJwtTokenService

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository

    @Autowired
    lateinit var appConfig: AppConfig

    @GetMapping("/auth/validate/{email}")
    fun generate(@PathVariable email: String): ResponseEntity<Map<String, Any>> {
        val admin = adminUserRepository.findByEmail(email)?: throw ApiRequestException("admin_not_found")
        return ResponseEntity.ok(mapOf("token" to adminJwtTokenProvider.generateToken(AdminPrincipal(admin))))
    }


    @GetMapping("/deeplink/{runId}")
    fun generateDeeplink(@PathVariable runId: String): DeepLink {
       return DeepLink("${appConfig.baseUrl}/ios/run/${runId}")
    }

}

data class DeepLink(val url:String)