package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.rest.v1.controllers.support.*
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/admin/api/v1/support")
@RestController
class AdminSupportRestController {

    @Autowired
    lateinit var createSupport: CreateSupportController

    @Autowired
    lateinit var getSupportController: GetSupportController

    @Autowired
    lateinit var getAllSupportController: GetAllSupportController

    @Autowired
    lateinit var resolveSupportTicketController: ResolveSupportTicketController

    @PostMapping("/create")
    fun create(@RequestBody model: CreateSupportRequest): Support {
        return createSupport.execute(model)
    }

    @PostMapping("/resolve")
    fun resolve(@RequestBody model: ResolveSupportTicketModel): Support {
        return resolveSupportTicketController.execute(model)
    }

    @GetMapping("/list")
    fun getUser(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @RequestParam(defaultValue = "25") size: Int): List<Support> {
        val pageable = PageRequest.of(
            page,
            size.coerceAtMost(100),
            Sort.by("created_at"))
        return getAllSupportController.execute(GetAllSupportModel(pageable, status, date))
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): Support {
        return getSupportController.execute(id)
    }

}