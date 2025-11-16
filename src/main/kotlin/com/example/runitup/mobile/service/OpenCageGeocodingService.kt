package com.example.runitup.mobile.service

import com.example.runitup.mobile.rest.v1.dto.GeocodeResult
import com.example.runitup.mobile.rest.v1.dto.OpenCageResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

interface GeocodingService {
    fun geocode(address: String): GeocodeResult
}

@Service
class OpenCageGeocodingService(
    @Value("\${geocoding.opencage.base-url}") private val baseUrl: String,
    @Value("\${geocoding.opencage.api-key}") private val apiKey: String
) : GeocodingService {

    private val logger = myLogger()

    @Autowired
    @Qualifier("geocode")
    lateinit var webClient: WebClient

    override fun geocode(address: String): GeocodeResult {
        if (address.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "address must not be blank")
        }

        val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/geocode/v1/json")
            .queryParam("q", address)           // raw address with spaces is OK
            .queryParam("key", apiKey)
            .queryParam("limit", 1)
            .queryParam("no_annotations", 0)    // so we get timezone info
            .build()
            .encode()                           // <-- THIS encodes spaces, commas, etc.
            .toUriString()

        val response = webClient.get()
            .uri(uri)
            .retrieve()
            .onStatus({ it.is4xxClientError }) {
                it.bodyToMono<String>().map { body ->
                    logger.error("Geocoding request failed address = {}, body = {}", address, body)
                    ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Geocoding request failed: $body"
                    )
                }
            }
            .onStatus({ it.is5xxServerError }) {
                it.bodyToMono<String>().map { body ->
                    logger.error("Geocoding service unavailable address = {}, body = {}", address, body)
                    ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Geocoding service unavailable: $body"
                    )
                }
            }
            .bodyToMono<OpenCageResponse>()
            .block()
            ?: run {
                logger.error("Empty geocoding response address = {}", address)
                throw ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Empty geocoding response"
                )
            }

        val first = response.results.firstOrNull()
            ?: run {
                logger.error("No results for the provided address = {}", address)
                throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No results for the provided address"
                )
            }


        return GeocodeResult(
            latitude = first.geometry.lat,
            longitude = first.geometry.lng,
            formattedAddress = first.formatted ?: address,
            annotations = first.annotations
        )
    }
}
