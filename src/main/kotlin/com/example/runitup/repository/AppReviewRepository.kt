package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.AppReview
import com.example.runitup.model.Gym
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.awt.Point

@Repository
@Document(collection = CollectionConstants.REVIEW)
interface AppReviewRepository : MongoRepository<AppReview, String> {}