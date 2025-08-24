package com.example.runitup.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint

class Gym (
    @Id var id: String,
    var address: String,
    var location: GeoJsonPoint?,
    var fee: Double,
    var phoneNumber: String?,
    var city: String,
    var title: String,
    var state: String,
    var notes: String,
    var description: String,
    var country:String = "USA",
    var zipCode:Long
): BaseModel()