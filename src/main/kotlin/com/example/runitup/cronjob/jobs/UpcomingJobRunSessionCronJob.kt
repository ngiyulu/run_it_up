package com.example.runitup.cronjob.jobs


import com.example.runitup.cronjob.CronJobRunner
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.service.RunSessionTimeWindowService
import com.example.runitup.mobile.service.TimeService
import com.example.runitup.mobile.service.myLogger
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class UpcomingJobRunSessionCronJob(
    private val runner: CronJobRunner,
    private val runSessionPushNotificationService: RunSessionPushNotificationService,
    private val timeWindowService: RunSessionTimeWindowService) {
    private val logger = myLogger()

    // it will run ever 25 mins
    @Scheduled(cron = "0 */45 * * * *")
    fun schedule() {
        runner.runCron(jobName = "upcoming-run-session job", ttlSeconds = 300) { audit ->
            logger.info("upcoming-run-session job is running")
            // example job body — NEVER blocks HTTP threads
            withContext(Dispatchers.IO) {
                val runSession = timeWindowService.processOneHourBeforeRunSession()
                logger.info("${runSession.count()} upcoming runs will be notified")
                notifyPlayers(runSession)
                // Add any custom metadata
                audit.putMeta("note", "process bath of jobs where users need to be notified that a run is starting soon")
            }
        }
    }

    private fun notifyPlayers(list: List<RunSession>){
        list.forEach { run->
            val bookingList = run.bookings
            logger.info("we found ${bookingList.size} bookings for run ${run.id.orEmpty()}")
            bookingList.forEach {
                runSessionPushNotificationService.runSessionAboutToStart(it.userId, run, it.id.orEmpty())
            }
        }
    }
}