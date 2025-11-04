package com.example.runitup.mobile.model

import com.example.runitup.mobile.constants.CollectionConstants
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository


@Repository
@Document(collection = CollectionConstants.CRON_LOG_COLLECTION)
interface CronRunLogRepository : MongoRepository<CronRunLog, String> {
}