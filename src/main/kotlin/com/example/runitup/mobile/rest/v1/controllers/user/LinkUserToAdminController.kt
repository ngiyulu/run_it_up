package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.UserType
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class LinkUserToAdminController: BaseController<LinkUserToAdminModel, User>() {
    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository

    @Autowired
    lateinit var userRepository: UserRepository

    override fun execute(request: LinkUserToAdminModel): User {
        val auth = SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        val user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        val adminDb = adminUserRepository.findById(request.admin)
        if(!adminDb.isPresent){
            throw ApiRequestException(text("admin_not_found"))
        }

        val admin = adminDb.get()
        val existingUser = userRepository.findByLinkedAdmin(admin.id.orEmpty())
        if(existingUser != null){
            throw  ApiRequestException("admin already linked")
        }
        user.linkedAdmin = admin.id
        user.userType = UserType.ADMIN
        user.stripeId?.let {
            user.payments = paymentService.listOfCustomerCards(it)
        }
        return  cacheManager.updateUser(user)

    }
}

class LinkUserToAdminModel(val admin:String)