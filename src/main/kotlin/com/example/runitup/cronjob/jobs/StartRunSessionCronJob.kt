package com.example.runitup.cronjob.jobs


import com.example.runitup.cronjob.CronJobRunner
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.session.StartSessionModel
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.StartRunSessionModelEnum
import com.example.runitup.mobile.service.TimeService
import com.example.runitup.mobile.service.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class StartRunSessionCronJob(
    private val runner: CronJobRunner,
    private val runSessionRepository: RunSessionRepository,
    private  val sessionService: RunSessionService,
    private val timeService: TimeService) {
    private val logger = myLogger()

    // it will run every 10 mins
    @Scheduled(cron = "0 */10 * * * *")
    fun schedule() {
        runner.runCron(jobName = "start-run-session", ttlSeconds = 300) { audit ->
            // example job body — NEVER blocks HTTP threads
            withContext(Dispatchers.IO) {
                val session = runSessionRepository.findByStatus(RunStatus.CONFIRMED)
                session.forEach {
                    val shouldProcess =timeService.shouldSessionStart(
                        it.date,
                        it.startTime,
                        ZoneId.of(it.zoneId)
                    )
                    if(shouldProcess && !it.lockStart){
                        val model = sessionService.startRunSession(StartSessionModel(it.id.orEmpty(), false), it)
                        when (model.status) {
                            StartRunSessionModelEnum.INVALID_ID -> {
                                throw ApiRequestException("invalid id")
                            }
                            StartRunSessionModelEnum.CONFIRMED -> {
                                throw ApiRequestException("job already confirmed")
                            }
                            else -> {
                                logger.info("session ${it.id.orEmpty()} was started successfully from cron job")
                            }
                        }
                        audit.incrementProcessedBy(1)
                    }
                }
                // Add any custom metadata
                audit.putMeta("note", "process bath of jobs that needed to be completed")
            }
        }
    }
}
