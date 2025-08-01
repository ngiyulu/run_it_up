package com.example.runitup.service.auth

import com.example.runitup.service.PasswordValidator
import com.example.runitup.service.TextService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class BaseAuthService {

    @Autowired
    lateinit var passwordValidator: PasswordValidator

    @Autowired
    lateinit var textService: TextService

    fun generateCode(): String{
        return UUID.randomUUID().toString().substring(0, 8)
    }

    fun  text(code:String, locale:String): String{
        return textService.getText(code, locale)
    }

}