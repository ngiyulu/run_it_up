package com.example.runitup.controller

import com.example.runitup.cache.MyCacheManager
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.LoggerService
import com.example.runitup.service.TextService
import com.example.runitup.service.TimeAndDateService
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
    protected lateinit var timeAndDateService: TimeAndDateService

    @Autowired
    protected lateinit var textService: TextService

    @Autowired
    protected lateinit var logger: LoggerService

    abstract fun execute(request: R): P

    fun getTimeStamp(): Long{
        return timeAndDateService.getTimeStamp()
    }

    fun getUser(): User{
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