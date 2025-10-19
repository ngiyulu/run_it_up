package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.rest.v1.controllers.appreview.CreateAppReviewController
import com.example.runitup.mobile.rest.v1.dto.CreateAppReviewRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/app-review")
class AppReviewRestController {

    @Autowired
    lateinit var createAppReviewController: CreateAppReviewController

    @PostMapping("/create")
    fun create(@RequestBody model: CreateAppReviewRequest): com.example.runitup.mobile.model.AppReview {
        return createAppReviewController.execute(model)
    }

}