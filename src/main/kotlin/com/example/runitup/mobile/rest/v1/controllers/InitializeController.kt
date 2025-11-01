package com.example.runitup.mobile.rest.v1.controllers

import com.example.runitup.mobile.config.PaymentConfig
import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.extensions.convertToPhoneType
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.dto.GuideLine
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeResponse
import com.example.runitup.mobile.security.JwtTokenService
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.SendGridService
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
    lateinit var paymentConfig: PaymentConfig
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


        val guidelines = listOf(
            GuideLine(
                title = "Respect All Players",
                description = "Treat everyone with respect — regardless of skill level, gender, or background. Trash talk is part of the game, but disrespect or harassment is never tolerated.",
                image = "hand.raised.fill"
            ),
            GuideLine(
                title = "Show Up or Cancel Early",
                description = "If you’ve joined a run, show up on time. Can’t make it? Cancel early so someone else can take your spot — no-shows hurt the whole community.",
                image = "calendar.badge.clock"
            ),
            GuideLine(
                title = "No Discrimination or Hate Speech",
                description = "RunItUp is a diverse community. Discriminatory or hateful language or actions will result in immediate suspension.",
                image = "hand.raised.slash.fill"
            ),
            GuideLine(
                title = "Maintain Cleanliness",
                description = "Respect the space. Pick up after yourself, keep the court clean, and leave locker rooms as you found them.",
                image = "sparkles"
            ),
            GuideLine(
                title = "Report Issues",
                description = "If something feels wrong — from unsafe play to harassment — report it through the app. Your feedback helps keep the community safe.",
                image = "exclamationmark.bubble.fill"
            ),
            GuideLine(
                title = "Be Honest in Your Skill Level",
                description = "Choose your skill level accurately when joining runs. It helps match players fairly and keeps games competitive yet fun.",
                image = "figure.basketball"
            ),
            GuideLine(
                title = "Encourage Others",
                description = "Support newer players, celebrate good plays, and help build a positive environment that keeps everyone coming back.",
                image = "hands.sparkles.fill"
            )
        )
        return InitializeResponse(gyms, user, token.orEmpty(), true, 3, "", "", false, guidelines).apply {
            if(request.os.convertToPhoneType() == PhoneType.ANDROID){
                this.allowedPayment = paymentConfig.paymentAndroid
            }
            else{
                this.allowedPayment = paymentConfig.paymentIos
            }
        }
    }
}