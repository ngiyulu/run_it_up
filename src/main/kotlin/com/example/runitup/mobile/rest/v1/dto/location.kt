package com.example.runitup.mobile.rest.v1.dto

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String
)

// OpenCage API response (minimal fields we need)
data class OpenCageResponse(
    val results: List<OpenCageResult> = emptyList()
)

data class OpenCageResult(
    val geometry: Geometry,
    val formatted: String?
)

data class Geometry(
    val lat: Double,
    val lng: Double
)