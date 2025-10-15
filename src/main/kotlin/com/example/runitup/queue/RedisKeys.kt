package com.example.runitup.queue

object RedisKeys {
    fun ready(q: String) = "q:$q:ready"
    fun delayed(q: String) = "q:$q:delayed"
    fun inflight(q: String) = "q:$q:inflight"
    fun msg(q: String, id: String) = "q:$q:msg:$id"
    fun receipt(q: String, rh: String) = "q:$q:receipt:$rh"
    fun cfg(q: String) = "q:$q:cfg"
    fun dlq(q: String) = "q:$q:dlq"
}