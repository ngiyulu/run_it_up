package com.example.runitup.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class TextService: BaseService() {
    @Autowired
    lateinit var messageSource: MessageSource
    fun  getText(key:String, locale:String): String{
        val localeObject = Locale.forLanguageTag(locale)
        return messageSource.getMessage(key, null, localeObject)
    }

    fun  getTextWithPlaceHolder(code:String, name:Array<String>,  locale:String): String{
        val localeObject = Locale.forLanguageTag(locale)
        return messageSource.getMessage(code, name, localeObject)
    }
}