// src/test/kotlin/com/example/runitup/mobile/service/JobTrackerServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.JobExecution
import com.example.runitup.mobile.model.JobStatus
import com.example.runitup.mobile.repository.JobExecutionRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class JobTrackerServiceTest {

    private val repo = mockk<JobExecutionRepository>()
    private val service = JobTrackerService(repo)

    @AfterEach fun tearDown() { clearAllMocks(); unmockkAll() }

    @Test
    fun `start persists new JobExecution with provided fields`() {
        val savedSlot = slot<JobExecution>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val result = service.start(
            jobId = "job-1",
            queue = "email-queue",
            taskType = "SEND_EMAIL",
            workerId = "worker-A",
            attempt = 2,
            receiveCount = 5,
            traceId = "trace-xyz"
        )

        // verify save invoked exactly once
        verify(exactly = 1) { repo.save(any<JobExecution>()) }

        // assert values passed to repo.save
        val je = savedSlot.captured
        assertThat(je.jobId).isEqualTo("job-1")
        assertThat(je.queue).isEqualTo("email-queue")
        assertThat(je.taskType).isEqualTo("SEND_EMAIL")
        assertThat(je.workerId).isEqualTo("worker-A")
        assertThat(je.attempt).isEqualTo(2)
        assertThat(je.receiveCount).isEqualTo(5)
        assertThat(je.traceId).isEqualTo("trace-xyz")

        // the service returns whatever repo.save returns
        assertThat(result).isSameAs(je)
    }

    @Test
    fun `success sets status to SUCCEEDED and finishedAt then saves`() {
        val existing = JobExecution(
            id = "exec-1",
            jobId = "job-2",
            queue = "q",
            taskType = "T",
            workerId = "W",
            attempt = 1,
            receiveCount = 1,
            traceId = null
        )
        every { repo.findById("exec-1") } returns Optional.of(existing)
        every { repo.save(any<JobExecution>()) } answers { firstArg() }

        service.success("exec-1")

        verify(exactly = 1) { repo.findById("exec-1") }
        val savedSlot = slot<JobExecution>()
        verify(exactly = 1) { repo.save(capture(savedSlot)) }

        val saved = savedSlot.captured
        assertThat(saved.status).isEqualTo(JobStatus.SUCCEEDED)
        assertThat(saved.finishedAt).isNotNull()
        // finishedAt should be "now-ish"
        assertThat(saved.finishedAt!!).isBeforeOrEqualTo(Instant.now().plusSeconds(2))
    }

    @Test
    fun `success silently returns when execution not found`() {
        every { repo.findById("missing") } returns Optional.empty()
        // no save should happen
        service.success("missing")
        verify(exactly = 1) { repo.findById("missing") }
        verify(exactly = 0) { repo.save(any<JobExecution>()) }
    }

    @Test
    fun `failure sets FAILED and captures error details then saves`() {
        val existing = JobExecution(
            id = "exec-2",
            jobId = "job-3",
            queue = "q",
            taskType = "T",
            workerId = "W",
            attempt = 3,
            receiveCount = 7,
            traceId = "trace"
        )
        every { repo.findById("exec-2") } returns Optional.of(existing)
        every { repo.save(any<JobExecution>()) } answers { firstArg() }

        val boom = IllegalStateException("bad things happened")

        service.failure("exec-2", boom)

        verify(exactly = 1) { repo.findById("exec-2") }
        val savedSlot = slot<JobExecution>()
        verify(exactly = 1) { repo.save(capture(savedSlot)) }

        val saved = savedSlot.captured
        assertThat(saved.status).isEqualTo(JobStatus.FAILED)
        assertThat(saved.finishedAt).isNotNull()
        assertThat(saved.errorType).isEqualTo(IllegalStateException::class.java.name)
        assertThat(saved.errorMessage).isEqualTo("bad things happened")
        assertThat(saved.errorStack).contains("IllegalStateException")
        assertThat(saved.errorStack).contains("bad things happened")
    }

    @Test
    fun `failure silently returns when execution not found`() {
        every { repo.findById("missing") } returns Optional.empty()
        service.failure("missing", RuntimeException("x"))
        verify(exactly = 1) { repo.findById("missing") }
        verify(exactly = 0) { repo.save(any<JobExecution>()) }
    }
}
