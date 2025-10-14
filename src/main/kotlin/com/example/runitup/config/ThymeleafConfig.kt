package com.example.runitup.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
class ThymeleafConfig {

    @Bean
    fun classLoaderTemplateResolver(): ClassLoaderTemplateResolver {
        return ClassLoaderTemplateResolver().apply {
            prefix = "templates/"            // classpath:templates
            suffix = ".html"                 // your templates end with .html
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
            isCacheable = true               // turn false in dev if you want hot reloads
            order = 1
        }
    }

}
