package com.example.runitup.mobile.rest.v1.controllers

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.TextService
import com.google.cloud.firestore.telemetry.MetricsUtil.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
abstract class BaseController<R, P> {
    @Autowired
    protected lateinit var cacheManager: MyCacheManager

    @Autowired
    protected lateinit var textService: TextService

    var TAG = this.javaClass.simpleName

    abstract fun execute(request: R): P

    fun getTimeStamp(): Long{
        return Instant.now().epochSecond
    }

    fun getUser(): User {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
       return cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
    }
    fun  text(code:String): String{
        return textService.getText(code, LocaleContextHolder.getLocale().toString())
    }
    fun  text(code:String, placerHolders:Array<String>): String{
        return textService.getTextWithPlaceHolder(code, placerHolders, LocaleContextHolder.getLocale().toString())
    }


}