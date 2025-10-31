package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.mobile.rest.v1.dto.GeocodeResult
import com.example.runitup.mobile.service.GeocodingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/geocode")
class GeocodeRestController(
    private val geocodingService: GeocodingService
) {

    @PostMapping("/retrieve")
    fun geocode(@RequestBody body: GetCoordinateRequest): ResponseEntity<GeocodeResult> {
        val result = geocodingService.geocode(body.address)
        return ResponseEntity.ok(result)
    }
}

class GetCoordinateRequest(val address: String)
