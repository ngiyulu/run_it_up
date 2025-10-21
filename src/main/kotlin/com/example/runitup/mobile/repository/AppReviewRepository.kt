package com.example.runitup.mobile.repository


import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.AppReview
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.REVIEW)
interface AppReviewRepository : MongoRepository<AppReview, String> {}