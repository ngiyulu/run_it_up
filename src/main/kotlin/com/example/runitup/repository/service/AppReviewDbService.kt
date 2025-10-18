package com.example.runitup.repository.service

import com.example.runitup.model.AppReview
import com.example.runitup.repository.AppReviewRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AppReviewDbService {

    @Autowired
    lateinit var appReviewRepository: AppReviewRepository

    fun createAppReview(review: AppReview): AppReview{
        return appReviewRepository.save(review)
    }

}