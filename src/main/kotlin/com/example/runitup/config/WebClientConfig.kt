// src/main/kotlin/com/example/sms/WebClientConfig.kt
package com.example.runitup.config

import com.example.runitup.clicksend.ClickSendProperties
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfig(
    private val props: ClickSendProperties
) {

    @Bean
    fun clickSendWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeoutMs.toInt())
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(props.timeoutMs, TimeUnit.MILLISECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(props.timeoutMs, TimeUnit.MILLISECONDS))
            }

        return WebClient.builder()
            .baseUrl(props.baseUrl)
            .filter(ExchangeFilterFunctions.basicAuthentication(props.username, props.apiKey))
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
