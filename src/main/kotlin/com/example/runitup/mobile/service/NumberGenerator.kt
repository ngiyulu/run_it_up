package com.example.runitup.mobile.service

import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors


@Service
class NumberGenerator {

    fun generateCode(length: Long): String{
        val code: String = ThreadLocalRandom.current()
            .ints(length, 0, 10)
            .mapToObj(java.lang.String::valueOf)
            .collect(Collectors.joining())
        return code
    }
}