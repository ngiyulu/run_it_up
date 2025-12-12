package com.example.runitup.mobile.rest.v1.controllers

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.convertToPhoneType
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.repository.UserActionRequiredRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.dto.UserStat
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.mobile.rest.v1.dto.initialize.InitializeResponse
import com.example.runitup.mobile.security.JwtTokenService
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.UserStatsService
import com.example.runitup.mobile.service.WaiverService
import com.example.runitup.mobile.utility.AgeUtil
import com.example.runitup.mobile.utility.GuideLineUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Admin
import org.springframework.context.i18n.LocaleContextHolder
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
    lateinit var userStatsService: UserStatsService

    @Autowired
    lateinit var waiverService: WaiverService




    @Autowired
    lateinit var appConfig: AppConfig
    override fun execute(request: InitializeRequest): InitializeResponse {
        val gyms = gymRepository.findAll()
        var user: User? = null
        var token:String? = null
        var stats: UserStat? = null
        if(request.userId != null){
            user = cacheManager.getUser(request.userId)
            if(user?.isActive  == false){
                user = null
            }
            user?.let {
                val age = AgeUtil.ageFrom(user.dob, zoneIdString = request.zoneId.orEmpty())
                waiverService.setWaiverData(user, age)
                user.stripeId?.let {
                    user.payments = paymentService.listOfCustomerCards(it)
                }
                stats = userStatsService.getUserStats(user.id.orEmpty())
                //user.actions = actionRequiredRepository.findByUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(user.id.orEmpty(), listOf(ActionStatus.PENDING))
                token = jwtService.generateToken(UserPrincipal(user))
            }
            if(request.tokenModel != null){
                phoneService.createPhone(request.tokenModel, request.os, request.userId)
            }
        }
        var adminUser: AdminUser? = null
        sendEmail()
        user?.linkedAdmin?.let {
            adminUser = cacheManager.getAdmin(it)
        }
        var deeplink = appConfig.baseUrl+"/ios/run"
        if(request.os.convertToPhoneType() == PhoneType.ANDROID){
            deeplink = appConfig.baseUrl+"/android/run"
        }
        return InitializeResponse(gyms, user, token.orEmpty(), true, 3, "", deeplink, appConfig.email, false,  GuideLineUtil.provideGuideLineList(), userStats = stats, refundUrl = appConfig.refundUrl, appConfig.messaging, adminUser, appConfig.privacyUrl, appConfig.termsAndConditionUrl, appConfig.displayDays).apply {
            if(request.os.convertToPhoneType() == PhoneType.ANDROID){
                this.allowedPayment = appConfig.paymentAndroid
            }
            else{
                this.allowedPayment = appConfig.paymentIos
            }
        }
    }

    fun sendEmail(){
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
    }
}