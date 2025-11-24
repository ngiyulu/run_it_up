package com.example.runitup.mobile.rest.v1.restcontroller.dev

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.service.NearbyUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles
@RequestMapping("/api/dev/user")
class DevUserRestController {

    @Autowired
    lateinit var nearbyUserService: NearbyUserService

    @Autowired
    lateinit var sessionRepository: RunSessionRepository

    @GetMapping("/near/{sessionId}/{radius}")
    fun getUser(@PathVariable sessionId: String, @PathVariable radius: Double): List<User> {
        val sessionDb = sessionRepository.findById(sessionId)
        if(!sessionDb.isPresent){
            throw  ApiRequestException("session_not_found")
        }
        val session = sessionDb.get()
        return nearbyUserService.findUsersNearRunSession(session, radius)
    }
}