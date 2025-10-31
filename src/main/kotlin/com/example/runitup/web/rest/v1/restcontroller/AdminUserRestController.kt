package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.dto.CreateUserRequest
import com.example.runitup.mobile.service.TextService
import com.example.runitup.web.rest.v1.controller.user.CreateUserAdminController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/admin/api/v1/user")
class AdminUserRestController {

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Autowired
    lateinit var createUserAdminController: CreateUserAdminController

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var textService: TextService

    @GetMapping("/retrieve/{id}")
    fun getUser(@PathVariable id: String): User {
        return cacheManager.getUser(id) ?: throw ApiRequestException(textService.getText("user_not_found", LocaleContextHolder.getLocale().toString()))
    }
    @GetMapping("/retrieve/email/{email}")
    fun getUserByEmail(@PathVariable email: String): User {
        return userRepository.findByEmail(email) ?: throw ApiRequestException(textService.getText("user_not_found", LocaleContextHolder.getLocale().toString()))
    }

    @PostMapping("/create")
    @Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles

    fun createUser(
        @RequestBody model: CreateUserRequest,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
    ): User {
        return createUserAdminController.execute(Pair(tzHeader,model))
    }

    @GetMapping("/list")
    fun getUser(
        @RequestParam(required = false) verified: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int): List<User> {
        val pageable = PageRequest.of(
            page,
            size.coerceAtMost(100),
            Sort.by("first"))
        if(verified == null){
            return userRepository.findAll(pageable).content
        }
        return userRepository.findAllByVerifiedPhone(verified, pageable).content

    }

}