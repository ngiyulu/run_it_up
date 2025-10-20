package com.example.runitup.mobile.crash

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    private val log = LoggerFactory.getLogger(AsyncConfig::class.java)

    override fun getAsyncExecutor(): Executor {
        val exec = ThreadPoolTaskExecutor()
        exec.corePoolSize = 4
        exec.maxPoolSize = 16
        exec.setThreadNamePrefix("async-")
        exec.setTaskDecorator(MdcTaskDecorator())
        exec.initialize()
        return exec
    }

    override fun getAsyncUncaughtExceptionHandler() = AsyncUncaughtExceptionHandler { ex, method, params ->
        log.error("Uncaught @Async error in ${method.declaringClass.simpleName}.${method.name}(${params.joinToString()})", ex)
    }
}

class MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap()
        return Runnable {
            if (contextMap != null) MDC.setContextMap(contextMap) else MDC.clear()
            try { runnable.run() } finally { MDC.clear() }
        }
    }
}
