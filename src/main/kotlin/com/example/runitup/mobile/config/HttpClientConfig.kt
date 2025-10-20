package com.example.runitup.mobile.config

import constant.ServiceConstant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@ConfigurationProperties(prefix = "downstream")
data class DownstreamProps(
    var serviceMessaging: ServiceProps = ServiceProps()) {
    data class ServiceProps(var baseUrl: String = "", var connectTimeoutMs: Int = 3000, var readTimeoutMs: Int = 5000)
}

@Configuration
@EnableConfigurationProperties(DownstreamProps::class)
class HttpClientConfig(
    private val props: DownstreamProps
) {
    @Bean
    @Qualifier(ServiceConstant.messagingService)
    fun serviceAClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl(props.serviceMessaging.baseUrl)
            .clientConnector(reactorConnector(props.serviceMessaging))
            .filter(commonLoggingFilter(ServiceConstant.messagingService))
            .build()

    private fun reactorConnector(p: DownstreamProps.ServiceProps): ReactorClientHttpConnector {
        val httpClient = reactor.netty.http.client.HttpClient.create()
            .responseTimeout(java.time.Duration.ofMillis(p.readTimeoutMs.toLong()))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, p.connectTimeoutMs)
        return ReactorClientHttpConnector(httpClient)
    }

    private fun commonLoggingFilter(tag: String) = ExchangeFilterFunction { req, next ->
        val start = System.currentTimeMillis()
        next.exchange(req).flatMap { res ->
            val dur = System.currentTimeMillis() - start
            LoggerFactory.getLogger("Outbound.$tag").info(
                "HTTP {} {} -> {} ({} ms)", req.method(), req.url(), res.statusCode().value(), dur
            )
            Mono.just(res)
        }
    }
}