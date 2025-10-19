package com.example.runitup.mobile.rest.v1.controllers.appreview

import com.example.runitup.mobile.model.AppReview
import com.example.runitup.mobile.repository.AppReviewRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateAppReviewRequest
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateAppReviewController: BaseController<CreateAppReviewRequest, AppReview>() {

    lateinit var appReviewRepository: AppReviewRepository

    override fun execute(request: CreateAppReviewRequest): AppReview {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return appReviewRepository.save(
            AppReview(
                star = request.star,
                userId = auth.id.orEmpty(),
                feedback = request.feedback
            )
        )
    }
}