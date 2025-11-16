package com.example.runitup.mobile.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint

class Gym (
    @Id var id: String,
    var line1: String,
    var line2: String,
    var location: GeoJsonPoint?,
    var zoneid:String,
    var image:String?,
    var fee: Double,
    var phoneNumber: String?,
    var city: String,
    var title: String,
    var state: String,
    var notes: String,
    var description: String,
    var country:String = "USA",
    var zipCode:String
): BaseModel(){
    fun getAddress():String{
        return "$line1, $line2, $city, $state $zipCode"
    }
}