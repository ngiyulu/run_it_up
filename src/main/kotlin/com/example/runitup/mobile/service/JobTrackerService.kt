package com.example.runitup.mobile.service

// service/JobTrackerService.kt
import com.example.runitup.mobile.model.JobExecution
import com.example.runitup.mobile.model.JobStatus
import com.example.runitup.mobile.repository.JobExecutionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

@Service
class JobTrackerService(
    private val repo: JobExecutionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun start(
        jobId: String,
        queue: String,
        taskType: String,
        workerId: String,
        attempt: Int,
        receiveCount: Int,
        traceId: String?
    ): JobExecution {
        val je = JobExecution(
            jobId = jobId,
            queue = queue,
            taskType = taskType,
            workerId = workerId,
            attempt = attempt,
            receiveCount = receiveCount,
            traceId = traceId
        )
        return repo.save(je)
    }

    fun success(executionId: String) {
        val je = repo.findById(executionId).orElse(null) ?: return
        je.status = JobStatus.SUCCEEDED
        je.finishedAt = Instant.now()
        repo.save(je)
    }

    fun failure(executionId: String, throwable: Throwable) {
        val je = repo.findById(executionId).orElse(null) ?: return
        je.status = JobStatus.FAILED
        je.finishedAt = Instant.now()
        je.errorType = throwable::class.java.name
        je.errorMessage = throwable.message
        je.errorStack = stackTrace(throwable)
        repo.save(je)
    }

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}
