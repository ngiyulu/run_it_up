package com.example.runitup

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.boot.web.context.WebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class StartupLogger(
    private val env: Environment,
    private val ctx: ApplicationContext,
    private val build: BuildProperties? = null,           // present if springBoot { buildInfo() }
    private val git: GitProperties? = null                // present if git-properties plugin applied
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        val port = (ctx as? WebServerApplicationContext)?.webServer?.port
            ?: env.getProperty("local.server.port", Int::class.java, -1)
        val profiles = env.activeProfiles.joinToString(", ").ifEmpty { "default" }
        val appName = build?.name ?: env.getProperty("spring.application.name", "application")
        val version = build?.version ?: "dev"
        val commit = git?.shortCommitId ?: git?.commitId?.take(8)

        log.info(
            "✅ {} v{} {}ready on port {} (profiles: {}).",
            appName,
            version,
            if (commit != null) "(commit $commit) " else "",
            port,
            profiles
        )
    }
}
