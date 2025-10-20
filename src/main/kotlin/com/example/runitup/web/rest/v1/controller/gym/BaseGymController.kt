package com.example.runitup.web.rest.v1.controller.gym

import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateGymRequest
import com.example.runitup.web.dto.GymCreateDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

abstract class BaseGymController: BaseController<CreateGymRequest, Gym>() {

    protected fun parseCreate(raw: String): GymCreateDto {
        val om = ObjectMapper()
        return  try { om.readValue(raw, GymCreateDto::class.java) }
        catch (e: Exception) { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload: ${e.message}") }
    }

}