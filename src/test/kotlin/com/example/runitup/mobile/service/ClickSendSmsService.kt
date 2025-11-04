// src/test/kotlin/com/example/runitup/mobile/service/ClickSendSmsServiceTest.kt
package com.example.runitup.mobile.service


import com.example.runitup.mobile.clicksend.ClickSendProperties
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

class ClickSendSmsServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ClickSendSmsService

    @BeforeEach
    fun setUp() {
        server = MockWebServer().also { it.start() }

        // ClickSend endpoints are under /v3 normally; our service uses relative paths like "/sms/send"
        val baseUrl = server.url("/v3").toString().removeSuffix("/")
        val client = WebClient.builder()
            .baseUrl(baseUrl)
            .build()

        val props = ClickSendProperties(
            username = "user",
            apiKey = "key",
            senderId = "RunItUp",
            enabled = true,
            baseUrl = baseUrl,
            timeoutMs = 5_000
        )

        service = ClickSendSmsService(client, props)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sendSmsDetailed - success maps fields and posts correct payload`() = runTest {
        // Given a typical ClickSend success envelope
        val body = """
            {
              "http_code": 200,
              "response_msg": "SUCCESS",
              "data": {
                "messages": [
                  { "status": "QUEUED", "message_id": "abc123" }
                ]
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body)
        )

        // When
        val to = "+15550001111"
        val message = "Hello there"
        val tag = "login-otp"
        val result = service.sendSmsDetailed(to, message, tag)

        // Then – response mapping
        assertThat(result.accepted).isTrue()
        assertThat(result.responseMsg).isEqualTo("SUCCESS")
        assertThat(result.providerStatus).isEqualTo("QUEUED")
        assertThat(result.messageId).isEqualTo("abc123")

        // And the request that was sent
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/v3/sms/send")
        assertThat(recorded.getHeader("Content-Type")).contains("application/json")

        val sentJson = recorded.body.readUtf8()
        // minimal payload assertions (don’t rely on field order)
        assertThat(sentJson).contains("\"to\":\"$to\"")
        assertThat(sentJson).contains("\"body\":\"$message\"")
        assertThat(sentJson).contains("\"from\":\"RunItUp\"")
        assertThat(sentJson).contains("\"custom_string\":\"$tag\"")
    }

    @Test
    fun `sendSmsDetailed - non-200 http_code marks accepted false`() = runTest {
        val body = """
            {
              "http_code": 400,
              "response_msg": "BAD_REQUEST",
              "data": { "messages": [ { "status": "FAILED", "message_id": "m1" } ] }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200) // HTTP is 200 but provider http_code != 200
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body)
        )

        val res = service.sendSmsDetailed("+15551230000", "oops", null)

        assertThat(res.accepted).isFalse()
        assertThat(res.responseMsg).isEqualTo("BAD_REQUEST")
        assertThat(res.providerStatus).isEqualTo("FAILED")
        assertThat(res.messageId).isEqualTo("m1")

        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/v3/sms/send")
    }

    @Test
    fun `fetchStatus - maps status from response data`() = runTest {
        val body = """
            {
              "data": {
                "status": "DELIVERED",
                "to": "+15550001111",
                "message_id": "abc123"
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body)
        )

        val status = service.fetchStatus("abc123")
        assertThat(status).isEqualTo("DELIVERED")

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(recorded.path).isEqualTo("/v3/sms/view/abc123")
    }

    @Test
    fun `fetchStatus - missing status returns UNKNOWN`() = runTest {
        val body = """{ "data": { } }"""

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body)
        )

        val status = service.fetchStatus("nope")
        assertThat(status).isEqualTo("UNKNOWN")
    }
}
