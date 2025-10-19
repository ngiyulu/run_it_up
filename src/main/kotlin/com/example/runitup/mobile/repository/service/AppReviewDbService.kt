package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.repository.AppReviewRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AppReviewDbService {

    @Autowired
    lateinit var appReviewRepository: AppReviewRepository

    fun createAppReview(review: com.example.runitup.mobile.model.AppReview): com.example.runitup.mobile.model.AppReview {
        return appReviewRepository.save(review)
    }

}