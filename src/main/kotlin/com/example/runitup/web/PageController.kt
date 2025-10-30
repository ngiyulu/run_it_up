package com.example.runitup.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {
    @GetMapping("/terms-and-conditions") fun termsAndConditions() = "terms-and-conditions"
    @GetMapping("/privacy") fun privacy() = "privacy"
    @GetMapping("/") fun home() = "home"
}