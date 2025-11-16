package com.example.runitup.mobile.rest.v1.dto

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val annotations: Annotations? = null
)

// OpenCage API response (minimal fields we need)
data class OpenCageResponse(
    val results: List<OpenCageResult> = emptyList()
)

data class OpenCageResult(
    val formatted: String?,
    val geometry: Geometry,
    val annotations: Annotations? = null
)

data class Geometry(
    val lat: Double,
    val lng: Double
)

data class Annotations(
    val timezone: TimeZoneAnnotation? = null
)

data class TimeZoneAnnotation(
    val name: String,
    val short_name: String? = null,
    val offset_sec: Int? = null,
    val offset_string: String? = null
)