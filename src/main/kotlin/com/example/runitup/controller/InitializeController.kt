package com.example.runitup.controller

import com.example.runitup.dto.initialize.InitializeRequest
import com.example.runitup.dto.initialize.InitializeResponse
import com.example.runitup.model.User
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.JwtService
import com.example.runitup.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InitializeController: BaseController<InitializeRequest, InitializeResponse>() {

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService
    override fun execute(request: InitializeRequest): InitializeResponse {
        val gyms = gymRepository.findAll()
        var user: User? = null
        var token:String? = null
        if(request.userId != null){
            val dbRes = userRepository.findById(request.userId)
            if(dbRes.isPresent){
                user = dbRes.get()
                token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            }
            request.tokenModel?.let {
                phoneService.createPhone(it)
            }
        }
        return  InitializeResponse(gyms, user, token.orEmpty(), 5)
    }
}