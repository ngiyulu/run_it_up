package com.example.runitup.mobile.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration


@Configuration
class MongoConfig(
    @Value("\${spring.data.mongodb.uri}")
    private val uri: String
) : AbstractMongoClientConfiguration() {

    override fun mongoClient(): MongoClient =
        MongoClients.create(uri)

    override fun getDatabaseName(): String =
        "main_db"              // or read from a property similarly

    // Optional: if your repositories live in a non‐default package
    // override fun getMappingBasePackages(): Collection<String> =
    //     listOf("com.yourcompany.yourapp.repositories")
}