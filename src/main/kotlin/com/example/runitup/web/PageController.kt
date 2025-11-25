package com.example.runitup.web

import com.example.runitup.mobile.config.AppConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {
    @Autowired
    lateinit var appConfig: AppConfig
    @GetMapping("/terms-and-conditions") fun termsAndConditions() = "terms-and-conditions"
    @GetMapping("/privacy") fun privacy() = "privacy"
    @GetMapping("/support") fun support() = "support"
    @GetMapping("/") fun home() = "home"
    @GetMapping("/refund") fun refund() = "refund"
    @GetMapping("/waiver")
    fun waiver(model: Model): String {
        model.addAttribute(
            "waiverUrl",
            appConfig.waiverUrl
        )
        return "download-waiver"
    }
}