package com.example.runitup.service

import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class TemplateRenderer(
    private val templateEngine: TemplateEngine
) {
    /**
     * Renders an HTML template from src/main/resources/templates/{templateName}.html
     * using the provided variables.
     *
     * Example: render("welcome", mapOf("name" to "Chris", "loginUrl" to "..."))
     */
    fun render(templateName: String, variables: Map<String, Any?>): String {
        val ctx = Context().apply {
            variables.forEach { (k, v) -> setVariable(k, v) }
        }
        return templateEngine.process(templateName, ctx)
    }
}
