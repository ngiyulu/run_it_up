package com.example.runitup.mobile.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.time.LocalDate

open class BaseModel(
    open var createdAt: LocalDate = LocalDate.now(),
    var updatedAt:Long = 0,
    private var version:Int = 1,
    @field:JsonProperty("isActive") // used for deserialization (field/ctor param)
    @get:JsonProperty("isActive")   // used for serialization (getter)
    @Field("isActive")              // stored as "isActive" in Mongo
    var isActive:Boolean = true
):
    Serializable {
    fun increment(){
        version ++
    }
}