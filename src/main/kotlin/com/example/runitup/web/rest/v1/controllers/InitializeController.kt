package com.example.runitup.web.rest.v1.controllers

import com.example.runitup.model.User
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import com.example.runitup.service.SendGridService
import com.example.runitup.web.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InitializeController: BaseController<InitializeRequest, InitializeResponse>() {

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var jwtService: JwtTokenService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var emailService: SendGridService
    override fun execute(request: InitializeRequest): InitializeResponse {
        val gyms = gymRepository.findAll()
        var user: User? = null
        var token:String? = null
        if(request.userId != null){
            val dbRes = userRepository.findById(request.userId)
            if(dbRes.isPresent){
                user = dbRes.get()
                user.stripeId?.let { it ->
                    user.payments = paymentService.listOfCustomerCards(it)
                }
                token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            }
            request.tokenModel?.let {
                phoneService.createPhone(it, request.os)
            }
        }
//        emailService.sendEmailHtml(
//            to = "cngiyulu@hotmail.com",
//            subject = "Welcome to RunItUp 🏀",
//            html = """
//        <div style="font-family:system-ui,Segoe UI,Roboto,Arial">
//          <h2 style="margin:0 0 8px">Welcome!</h2>
//          <p>Thanks for joining <strong>RunItUp</strong>.</p>
//          <p><a href="https://app.runitup.com/login">Sign in</a> to get started.</p>
//          <hr style="border:none;border-top:1px solid #eee;margin:16px 0">
//          <small>This is an automated message.</small>
//        </div>
//    """.trimIndent(),
//            plainTextFallback = "Welcome!\n\nThanks for joining RunItUp.\nSign in: https://app.runitup.com/login"
//        )
        return InitializeResponse(gyms, user, token.orEmpty(), true, 3)
    }
}