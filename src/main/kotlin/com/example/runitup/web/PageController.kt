package com.example.runitup.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {
    @GetMapping("/main/terms-and-conditions") fun termsAndConditions() = "terms-and-conditions"
    @GetMapping("/main/privacy") fun privacy() = "privacy"
    @GetMapping("/main/support") fun support() = "support"
    @GetMapping("/") fun home() = "home"
    @GetMapping("/main/refund") fun refund() = "refund"
}