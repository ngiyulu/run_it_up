package com.example.runitup.cronjob


import com.example.runitup.mobile.model.CronRunLog
import com.example.runitup.mobile.model.CronRunLogRepository
import com.example.runitup.mobile.model.CronStatus
import com.example.runitup.mobile.service.RedisLockService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.time.Instant
import java.util.*

@Service
class CronJobRunner(
    private val lockService: RedisLockService,
    private val runRepo: CronRunLogRepository,
    private val appScope: CoroutineScope,             // your existing application scope
    @Value("\${cron.lock.ttlSeconds:600}") private val defaultTtlSec: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val nodeId: String = InetAddress.getLocalHost().hostName

    /**
     * Runs [block] in a coroutine if we acquire a distributed lock.
     * Audits start/end + error. Non-blocking to HTTP threads.
     */
    fun runCron(
        jobName: String,
        ttlSeconds: Long = defaultTtlSec,
        meta: Map<String, String> = emptyMap(),
        block: suspend (AuditHandle) -> Unit
    ) {
        val lockKey = "cron:lock:$jobName"
        val token = UUID.randomUUID().toString()

        if (!lockService.tryLock(lockKey, ttlSeconds, token)) {
            log.debug("[$jobName] lock held elsewhere — skipping on $nodeId")
            return
        }

        appScope.launch(CoroutineName("cron-$jobName")) {
            val run = CronRunLog(
                jobName = jobName,
                startedAt = Instant.now(),
                nodeId = nodeId,
                meta = meta)
            runRepo.save(run)

            val audit = AuditHandle(runRepo, run.id)

            try {
                log.info("[$jobName] started on $nodeId")
                block(audit)
                audit.finishSuccess()
                log.info("[$jobName] finished SUCCESS on $nodeId")
            } catch (t: Throwable) {
                log.error("[$jobName] FAILED on $nodeId: ${t.message}", t)
                audit.finishFailure(t)
            } finally {
                lockService.unlock(lockKey, token)
            }
        }
    }

    /** Pass this to jobs so they can update itemsProcessed, extra metadata, etc. */
    class AuditHandle(
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
}
