package com.example.runitup.cronjob.jobs


import com.example.runitup.cronjob.CronJobRunner
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.service.TimeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class CompleteRunSessionCronJob(
    private val runner: CronJobRunner,
    private val runSessionRepository: RunSessionRepository,
    private val timeService: TimeService) {

    // it will run ever 25 mins
    @Scheduled(cron = "0 */30 * * * *")
    fun schedule() {
        runner.runCron(jobName = "complete-run-session", ttlSeconds = 300) { audit ->
            // example job body — NEVER blocks HTTP threads
            withContext(Dispatchers.IO) {
                val session = runSessionRepository.findByStatus(RunStatus.ONGOING)
                session.forEach {
                    val shouldProcess =timeService.hasEndedAtLeast(
                        it.date,
                        it.endTime,
                        ZoneId.of(it.zoneId),
                        30
                        )
                    if(shouldProcess){
                        it.status = RunStatus.COMPLETED
                        runSessionRepository.save(it)
                        audit.incrementProcessedBy(1)
                    }
                }
                // Add any custom metadata
                audit.putMeta("note", "process bath of jobs that needed to be completed")
            }
        }
    }
}
