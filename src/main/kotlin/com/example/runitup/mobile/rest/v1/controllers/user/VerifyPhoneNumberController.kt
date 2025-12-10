package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.clicksend.SmsService
import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.service.OtpDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.VerifyPhoneNumberRequest
import com.example.runitup.mobile.rest.v1.dto.VerifyPhoneNumberResponse
import com.example.runitup.mobile.security.JwtTokenService
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.EmailService
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.UserStatsService
import com.example.runitup.mobile.service.WaiverService
import com.example.runitup.mobile.utility.AgeUtil
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyPhoneNumberController: BaseController<VerifyPhoneNumberController.VerifyPhoneNumberControllerModel, VerifyPhoneNumberResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var otpDbService: OtpDbService

    @Autowired
    lateinit var jwtService: JwtTokenService

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var userStatsService: UserStatsService

    @Autowired
    lateinit var waiverService: WaiverService

    @Autowired
    lateinit var smsService: SmsService

    @Autowired
    lateinit var appConfig: AppConfig

    @Autowired
    lateinit var emailService: EmailService

    override fun execute(request: VerifyPhoneNumberControllerModel): VerifyPhoneNumberResponse {
        val enteredOtp = request.request.otp
        val otp = otpDbService.getOtp(request.request.phoneNumber)?: throw ApiRequestException(text("error"))
        print(otp)
        if(otp.code == enteredOtp){
            // this means the user has to create a new account
            if(otp.userId == null){
                return VerifyPhoneNumberResponse(true, null, null, null)
            }
            // this means user already has an account
            val user: User = cacheManager.getUser(otp.userId.orEmpty()) ?: throw ApiRequestException(text("invalid_user"))
            val token = jwtService.generateToken(UserPrincipal(user))
            otpDbService.disableOtp(otp)
            if(!user.isActive){
                throw ApiRequestException(textService.getText("inactive_user", LocaleContextHolder.getLocale().toString()))
            }
            val age = AgeUtil.ageFrom(user.dob, zoneIdString = request.zoneId)
            waiverService.setWaiverData(user, age)
            request.request.tokenModel?.let {
                phoneService.createPhone(it, request.os, user.id.orEmpty())
            }
            logger.info("otp object = $otp")
            runBlocking {
                if(appConfig.smsEnabled){
                    val response = smsService.sendSmsDetailed(otp.phoneNumber, "Your runitup Otp code is ${otp.code}. Do not share it with anyone")
                    logger.info("sms response for ${user.phoneNumber} = $response")
                }
                else{
                    emailService.sendOtpEmail(user.email, otp.code)
                }

            }
            return VerifyPhoneNumberResponse(true, user, token, userStatsService.getUserStats(user.id.orEmpty()))
        }
        return VerifyPhoneNumberResponse(false, null, null,  null)
    }


    class VerifyPhoneNumberControllerModel(val zoneId:String, val request: VerifyPhoneNumberRequest, val os:String, val model:String)
}