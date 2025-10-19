package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.service.CacheTestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class CacheRestController{
    @Autowired
    lateinit var cache: CacheTestService
    @GetMapping("/cache/ping")
    fun ping() = mapOf("pong" to cache.ping())

    @GetMapping("/cache/set")
    fun set(@RequestParam key: String, @RequestParam value: String) =
        mapOf("ok" to true).also { cache.set(key, value) }

    @GetMapping("/cache/get")
    fun get(@RequestParam key: String) =
        mapOf("key" to key, "value" to cache.get(key))
}