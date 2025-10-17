package com.example.runitup.web.rest.v1.restcontroller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AasaController(
    @Value("\${aasa.team-id}") private val teamId: String,
    @Value("\${aasa.bundle-id}") private val bundleId: String,
    @Value("\${aasa.paths:/}") private val pathsCsv: String
) {

    // Apple can query either path; serve both for safety.
    @GetMapping(
        value = ["/.well-known/apple-app-site-association", "/apple-app-site-association"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun aasa(): Map<String, Any> {
        val appId = "$teamId.$bundleId"
        val paths = pathsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return mapOf(
            // Universal Links
            "applinks" to mapOf(
                "apps" to emptyList<String>(),
                "details" to listOf(
                    mapOf(
                        "appID" to appId,
                        "paths" to paths
                    )
                )
            ),
            // Optional: Sign in with Apple / shared web credentials (Safari autofill)
            "webcredentials" to mapOf(
                "apps" to listOf(appId)
            ),
            // Optional: App Clips
            "appclips" to mapOf(
                "apps" to emptyList<String>()
            )
        )
    }
}
