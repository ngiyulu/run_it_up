package com.example.runitup.web


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin")
class AdminPageController {

    @GetMapping("/login") fun login() = "login"
    @GetMapping("/dashboard") fun dashboard() = "dashboard"

    @GetMapping("/gyms")
    fun gyms(): String = "gyms"

    @GetMapping("/runs")
    fun runs(): String = "runs"

    @GetMapping("/payments")
    fun payments(): String = "payments"

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/gyms/edit")
    fun editGym(@RequestParam id: String) = "gym-form" // same template, prefilled

    @GetMapping("/runsessions/create")
    fun createRunSession() = "run-session-form"

    @GetMapping("/runsessions/edit")
    fun editRunSession(@RequestParam id: String) = "run-session-form"

    @GetMapping("/gyms/create")
    fun createGym() = "gym-form" // creation form
}
