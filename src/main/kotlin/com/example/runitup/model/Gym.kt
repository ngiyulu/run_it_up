package com.example.runitup.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Field

class Gym (
    @Id var id: String,
    var address: String,
    var location: GeoJsonPoint?,
    var fee: Double,
    var phoneNumber: String?,
    var city: String,
    var state: String,
    var notes: String,
    var description: String,
    var country:String = "USA",
    var zipCode:Long
): BaseModel()