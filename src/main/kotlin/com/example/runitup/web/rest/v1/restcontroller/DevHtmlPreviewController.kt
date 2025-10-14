package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.service.TemplateRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles
@RestController
@RequestMapping("/api/_dev/html/")
class DevHtmlPreviewController {

    @Autowired
    lateinit var renderer: TemplateRenderer


    /**
     * POST /_dev/html/{template}
     * Content-Type: application/json
     * Body: { "name": "Chris", "loginUrl": "https://...", "year": 2025 }
     */
    @PostMapping("/{template}", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_HTML_VALUE])
    fun previewFromJson(
        @PathVariable template: String,
        @RequestBody variables: Map<String, Any?>
    ): ResponseEntity<String> {
        val html = renderer.render(template, variables)
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html)
    }
}