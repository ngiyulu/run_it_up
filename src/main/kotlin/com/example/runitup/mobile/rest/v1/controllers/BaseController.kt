package com.example.runitup.mobile.rest.v1.controllers

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionEventLogger
import com.example.runitup.mobile.service.TextService
import com.example.runitup.mobile.service.myLogger
import com.example.runitup.web.security.AdminPrincipal
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

    @Autowired
    protected lateinit var runSessionEventLogger: RunSessionEventLogger

    var TAG = this.javaClass.simpleName

    protected val logger = myLogger()

    abstract fun execute(request: R): P

    fun getTimeStamp(): Long{
        return Instant.now().epochSecond
    }

    fun getUser(): UserModel {
        val principal = SecurityContextHolder.getContext().authentication.principal
        if(principal is UserPrincipal){
            return UserModel(principal.user.id.orEmpty(), UserModelType.USER)
        }
        val auth = principal as AdminPrincipal
        return UserModel(auth.admin.id.orEmpty(), UserModelType.ADMIN)

    }

    fun getMyUser(): User {
        val principal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return  principal.user
    }

    fun getMyAdmin(): AdminUser {
        val principal = SecurityContextHolder.getContext().authentication.principal as AdminPrincipal
        return  principal.admin
    }


    fun  text(code:String): String{
        return textService.getText(code, LocaleContextHolder.getLocale().toString())
    }
    fun  text(code:String, placerHolders:Array<String>): String{
        return textService.getTextWithPlaceHolder(code, placerHolders, LocaleContextHolder.getLocale().toString())
    }


}
data class UserModel(val userId:String, val type: UserModelType)
enum class UserModelType{
    ADMIN, USER
}