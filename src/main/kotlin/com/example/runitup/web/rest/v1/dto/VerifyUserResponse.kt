package com.example.runitup.web.rest.v1.dto

import com.example.runitup.model.User


class VerifyUserResponse (val user: User?, val token: String? , val userId: String, val phone: String)