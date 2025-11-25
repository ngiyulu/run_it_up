package com.example.runitup.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {
    @GetMapping("/terms-and-conditions") fun termsAndConditions() = "terms-and-conditions"
    @GetMapping("/privacy") fun privacy() = "privacy"
    @GetMapping("/support") fun support() = "support"
    @GetMapping("/") fun home() = "home"
    @GetMapping("/refund") fun refund() = "refund"
    @GetMapping("/waiver") fun waiver() = "download-waiver"
}