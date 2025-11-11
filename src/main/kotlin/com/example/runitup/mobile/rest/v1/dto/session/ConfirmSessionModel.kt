package com.example.runitup.mobile.rest.v1.dto.session

class ConfirmSessionModel(val sessionId: String, var overrideMinimum:Boolean, val isAdmin:Boolean  =false)