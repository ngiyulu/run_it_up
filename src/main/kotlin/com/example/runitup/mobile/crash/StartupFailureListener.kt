// src/main/kotlin/com/example/config/StartupFailureListener.kt
package com.example.runitup.mobile.crash

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class StartupFailureListener : ApplicationListener<ApplicationFailedEvent> {
    private val log = LoggerFactory.getLogger(StartupFailureListener::class.java)
    override fun onApplicationEvent(event: ApplicationFailedEvent) {
        log.error("Spring Boot failed to start", event.exception)
    }
}
