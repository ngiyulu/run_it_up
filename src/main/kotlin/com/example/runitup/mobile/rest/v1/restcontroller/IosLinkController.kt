package com.example.runitup.mobile.rest.v1.restcontroller


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IosLinkController {
    // Catch-all for /ios/*
    @GetMapping("/ios/**")
    fun iosFallback(): String {
        // This will return src/main/resources/static/ios-fallback.html
        return "forward:/ios-fallback.html"
    }
}
