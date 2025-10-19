package com.example.runitup.mobile.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.*

@Configuration
class FirebaseConfig {

    @Value("\${firebase}") private val firebaseBase64: String = ""
    @Bean
    fun firebaseApp(): FirebaseApp {
        val credsBytes = Base64.getDecoder().decode(firebaseBase64)
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(credsBytes)))
            .build()
        return FirebaseApp.initializeApp(options)
    }
//        FileInputStream("src/main/resources/firebase-dev.json").use { serviceAccount ->
//            val options = FirebaseOptions.builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build()
//            FirebaseApp.initializeApp(options)
//        }

}