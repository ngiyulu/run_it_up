package com.example.runitup.mobile.rest.v1.controllers.appreview

import com.example.runitup.mobile.model.AppReview
import com.example.runitup.mobile.repository.AppReviewRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateAppReviewRequest
import org.springframework.stereotype.Service

@Service
class CreateAppReviewController: BaseController<CreateAppReviewRequest, AppReview>() {

    lateinit var appReviewRepository: AppReviewRepository

    override fun execute(request: CreateAppReviewRequest): AppReview {
        val user = getMyUser()
        return appReviewRepository.save(
            AppReview(
                star = request.star,
                userId = user.id.orEmpty(),
                feedback = request.feedback
            )
        )
    }
}