package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.rest.v1.dto.CreateGymRequest
import com.example.runitup.mobile.rest.v1.controllers.user.controller.gym.CreateGymController
import com.example.runitup.mobile.rest.v1.controllers.user.controller.gym.GetGym
import com.example.runitup.mobile.rest.v1.controllers.user.controller.gym.GetGymList
import com.example.runitup.mobile.rest.v1.controllers.user.controller.gym.UpdateGymController
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/admin/api/v1/gym")
@RestController
class AdminGymRestController {

    @Autowired
    lateinit var createGymController: CreateGymController

    @Autowired
    lateinit var getGymList: GetGymList

    @Autowired
    lateinit var getGym: GetGym

    @Autowired
    lateinit var updateGym: UpdateGymController


    /**
     * Create with multipart:
     *  - Part "payload" = JSON of GymCreateDto
     *  - Part "image"   = MultipartFile (required)
     */
    @PostMapping("/create", consumes = ["multipart/form-data"])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestPart("payload") @Valid payloadRaw: String,
        @RequestPart("image") image: MultipartFile
    ): Gym{
        return createGymController.execute(CreateGymRequest(payloadRaw, image, ""))
    }


    /**
     * Update (image optional):
     *  - Part "payload" = JSON of GymUpdateDto
     *  - Part "image"   = MultipartFile (optional)
     */
    @PutMapping("/update/{id}", consumes = ["multipart/form-data"])
    fun update(
        @PathVariable id: String,
        @RequestPart("payload") @Valid payloadRaw: String,
        @RequestPart("image", required = false) image: MultipartFile?
    ): Gym{
        return updateGym.execute(CreateGymRequest(payloadRaw, image, id))
    }


    @GetMapping("/list")
    fun getGymList(): List<Gym> {
        return getGymList.execute(Unit)
    }

    @GetMapping("/{gymId}")
    fun getGym(@PathVariable gymId: String): Gym {
        return getGym.execute(gymId)
    }

}