// src/test/kotlin/com/example/runitup/mobile/service/OpenCageGeocodingServiceTest.kt
package com.example.runitup.mobile.service

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
    fun `geocode returns GeocodeResult for valid address`() {
        // Arrange
        val body = """
        {
          "results": [
            {
              "geometry": {
                "lat": 33.069,
                "lng": -96.820
              },
              "formatted": "7850 Bishop Rd Apt 1301, Plano, TX 75024, United States",
              "annotations": {
                "timezone": {
                  "name": "America/Chicago"
                }
              }
            }
          ],
          "status": {
            "code": 200,
            "message": "OK"
          }
        }
    """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val address = "7850 bishop rd apt 1301, Plano, Tx 75024"

        // Act
        val result = service.geocode(address)

        // Assert
        val request = server.takeRequest()

        // Request path and query params
        assertThat(request.path).startsWith("/geocode/v1/json")

        val url = request.requestUrl!!
        val qParam = url.queryParameter("q")

        // ✅ Decode whatever we got back, then compare to the raw address
        val decodedQ = URLDecoder.decode(qParam, StandardCharsets.UTF_8)
        assertThat(decodedQ).isEqualTo(address)

        assertThat(url.queryParameter("key")).isEqualTo(apiKey)
        assertThat(url.queryParameter("limit")).isEqualTo("1")
        assertThat(url.queryParameter("no_annotations")).isEqualTo("0")

        // Response mapping
        assertThat(result.latitude).isEqualTo(33.069)
        assertThat(result.longitude).isEqualTo(-96.820)
        assertThat(result.formattedAddress)
            .isEqualTo("7850 Bishop Rd Apt 1301, Plano, TX 75024, United States")
        assertThat(result.annotations).isNotNull
    }

    @Test
    fun `geocode throws BAD_REQUEST when address is blank`() {
        // Act
        val ex = assertThrows<ResponseStatusException> {
            service.geocode("  ")
        }

        // Assert
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.reason).isEqualTo("address must not be blank")
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `geocode wraps 4xx responses as BAD_REQUEST`() {
        // Arrange
        val errorBody = """{"status":{"code":400,"message":"Invalid request"}}"""

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody)
        )

        // Act
        val ex = assertThrows<ResponseStatusException> {
            service.geocode("Some address")
        }

        // Assert
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.reason)
            .contains("Geocoding request failed:")
            .contains("Invalid request")

        // And we did call the server
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `geocode wraps 5xx responses as SERVICE_UNAVAILABLE`() {
        // Arrange
        val errorBody = """{"status":{"code":503,"message":"Service down"}}"""

        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody)
        )

        // Act
        val ex = assertThrows<ResponseStatusException> {
            service.geocode("Some address")
        }

        // Assert
        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(ex.reason)
            .contains("Geocoding service unavailable:")
            .contains("Service down")

        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `geocode throws SERVICE_UNAVAILABLE when response body is empty`() {
        // Arrange: 200 OK but no body
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "0")
        )

        // Act
        val ex = assertThrows<ResponseStatusException> {
            service.geocode("Some address")
        }

        // Assert
        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(ex.reason).isEqualTo("Empty geocoding response")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `geocode throws NOT_FOUND when no geocoding results`() {
        // Arrange: valid JSON, but empty results array
        val body = """
            {
              "results": [],
              "status": {
                "code": 200,
                "message": "OK"
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        // Act
        val ex = assertThrows<ResponseStatusException> {
            service.geocode("Some address")
        }

        // Assert
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(ex.reason).isEqualTo("No results for the provided address")
        assertThat(server.requestCount).isEqualTo(1)
    }
}
