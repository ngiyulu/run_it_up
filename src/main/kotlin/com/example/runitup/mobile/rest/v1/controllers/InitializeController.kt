package com.example.runitup.mobile.rest.v1.controllers

import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.extensions.convertToPhoneType
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.repository.UserActionRequiredRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeResponse
import com.example.runitup.mobile.security.JwtTokenService
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.SendGridService
import com.example.runitup.mobile.utility.GuideLineUtil
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

    @Autowired
    lateinit var actionRequiredRepository: UserActionRequiredRepository

    @Autowired
    lateinit var appConfig: AppConfig
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
                //user.actions = actionRequiredRepository.findByUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(user.id.orEmpty(), listOf(ActionStatus.PENDING))
                token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            }
            if(request.tokenModel != null){
                phoneService.createPhone(request.tokenModel, request.os, request.userId)
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

        return InitializeResponse(gyms, user, token.orEmpty(), true, 3, "", appConfig.baseUrl+"/ios/run", "", false,  GuideLineUtil.provideGuideLineList()).apply {
            if(request.os.convertToPhoneType() == PhoneType.ANDROID){
                this.allowedPayment = appConfig.paymentAndroid
            }
            else{
                this.allowedPayment = appConfig.paymentIos
            }
        }
    }
}