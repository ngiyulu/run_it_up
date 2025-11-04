package com.example.runitup.mobile.service


import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.myLogger(): Logger =
    LoggerFactory.getLogger(T::class.java)