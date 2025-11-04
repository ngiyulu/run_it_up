package com.example.runitup.cronjob

import com.example.runitup.mobile.model.CronRunLogRepository
import com.example.runitup.mobile.model.CronStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant


class CronJobAuditHandler(
    private val repo: CronRunLogRepository,
    private val id: String
) {
    suspend fun incrementProcessedBy(delta: Int) = withContext(Dispatchers.IO) {
        val doc = repo.findById(id).orElse(null) ?: return@withContext
        doc.itemsProcessed = (doc.itemsProcessed ?: 0) + delta
        repo.save(doc)
    }

    suspend fun putMeta(key: String, value: String) = withContext(Dispatchers.IO) {
        val doc = repo.findById(id).orElse(null) ?: return@withContext
        doc.meta = doc.meta + (key to value)
        repo.save(doc)
    }

    fun finishSuccess() {
        val doc = repo.findById(id).orElse(null) ?: return
        doc.status = CronStatus.SUCCESS
        doc.finishedAt = Instant.now()
        repo.save(doc)
    }

    fun finishFailure(t: Throwable) {
        val doc = repo.findById(id).orElse(null) ?: return
        doc.status = CronStatus.FAILED
        doc.errorMessage = t.message
        doc.errorStack = t.stackTraceToString()
        doc.finishedAt = Instant.now()
        repo.save(doc)
    }
}