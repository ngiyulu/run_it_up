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

    @GetMapping("/runsessions")
    fun runs(): String = "runs"

    @GetMapping("/users/view")
    fun userView(@RequestParam id: String): String = "user-detail"

    @GetMapping("/users")
    fun userList(): String = "users"

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

    @GetMapping("/waiver")
    fun waiver() = "waiver" // creation form

    @GetMapping("/support")
    fun supportList() = "support-list" // creation form

    @GetMapping("/support/detail")
    fun supportDetail(@RequestParam id: String) = "support-detail" // same template, prefilled

    @GetMapping("/push")
    fun pushMetrics(): String = "push-dashboard"
}
