package com.example.runitup.mobile.utility

import java.security.SecureRandom

object AppUtil {

    fun generate4DigitCode(): Int {
        val rng = SecureRandom()
        return 1000 + rng.nextInt(9000)
    }

}