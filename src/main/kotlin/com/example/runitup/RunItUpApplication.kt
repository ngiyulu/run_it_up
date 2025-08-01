package com.example.runitup

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RunItUpApplication

fun main(args: Array<String>) {
    runApplication<RunItUpApplication>(*args)
}
