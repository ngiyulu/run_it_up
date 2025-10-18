package com.example.runitup.web.rest.v1.controllers.appreview

import com.example.runitup.model.AppReview
import com.example.runitup.repository.AppReviewRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.CreateAppReviewRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateAppReviewController: BaseController<CreateAppReviewRequest, AppReview>() {

    lateinit var appReviewRepository: AppReviewRepository

    override fun execute(request: CreateAppReviewRequest): AppReview {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return appReviewRepository.save(AppReview(star = request.star, userId = auth.id.orEmpty(), feedback = request.feedback))
    }
}