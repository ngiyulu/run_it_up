package com.example.runitup.mobile.service

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class TextService( private val messageSource: MessageSource): BaseService() {
    fun  getText(key:String, locale:String): String{
        val localeObject = Locale.forLanguageTag(locale)
        return messageSource.getMessage(key, null, localeObject)
    }

    fun  getTextWithPlaceHolder(code:String, name:Array<String>,  locale:String): String{
        val localeObject = Locale.forLanguageTag(locale)
        return messageSource.getMessage(code, name, localeObject)
    }
}