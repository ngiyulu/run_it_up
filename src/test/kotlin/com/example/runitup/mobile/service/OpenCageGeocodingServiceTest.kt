// src/test/kotlin/com/example/runitup/mobile/service/OpenCageGeocodingServiceTest.kt
package com.example.runitup.mobile.service

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException

class OpenCageGeocodingServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OpenCageGeocodingService

    private val apiKey = "test-key"

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val baseUrl = server.url("/").toString().trimEnd('/')
        val wc = WebClient.builder().build()

        service = OpenCageGeocodingService(baseUrl = baseUrl, apiKey = apiKey).apply {
            this.webClient = wc
        }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `geocode success returns lat lng formatted`() {
        val body = """
            {
              "results": [
                {
                  "geometry": { "lat": 40.7128, "lng": -74.0060 },
                  "formatted": "New York, NY, USA"
                }
              ]
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val res = service.geocode("New York, NY")

        // verify parsed values
        assertThat(res.latitude).isEqualTo(40.7128)
        assertThat(res.longitude).isEqualTo(-74.0060)
        assertThat(res.formattedAddress).isEqualTo("New York, NY, USA")

        // verify request path & query params (double-encoded q)
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(recorded.path).startsWith("/geocode/v1/json?")
        assertThat(recorded.path).contains("key=$apiKey")
        assertThat(recorded.path).contains("q=New+York%252C+NY") // double-encoded value
        assertThat(recorded.path).contains("limit=1")
        assertThat(recorded.path).contains("no_annotations=1")
    }

    @Test
    fun `blank address throws BAD_REQUEST`() {
        val ex = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.geocode("  ")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.reason).contains("address must not be blank")
    }

    @Test
    fun `upstream 4xx mapped to BAD_REQUEST`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "text/plain")
                .setBody("invalid request")
        )

        val ex = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.geocode("bad address")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.reason).contains("Geocoding request failed")
    }

    @Test
    fun `upstream 5xx mapped to SERVICE_UNAVAILABLE`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "text/plain")
                .setBody("service down")
        )

        val ex = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.geocode("anything")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(ex.reason).contains("Geocoding service unavailable")
    }

    @Test
    fun `200 with empty body throws SERVICE_UNAVAILABLE`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("") // no body -> .block() returns null
        )

        val ex = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.geocode("Austin, TX")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(ex.reason).contains("Empty geocoding response")
    }

    @Test
    fun `200 with empty results throws NOT_FOUND`() {
        val body = """{ "results": [] }"""

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val ex = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.geocode("Nowhere 123")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(ex.reason).contains("No results for the provided address")
    }
}
