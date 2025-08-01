package com.example.runitup.service

//@Service
//class OtpService(
//    @Value("\${twilio.shortcode}") private val shortCode: String,
//    @Value("\${otp.expiration.minutes}") private val expiresInMins: Long
//) {
//    private data class Entry(val code: String, val expiresAt: Instant)
//    private val cache = ConcurrentHashMap<String, Entry>()
//    private val random = SecureRandom()
//
//    fun generateAndSend(to: String) {
//        val otp = "%06d".format(random.nextInt(1_000_000))
//        val expiry = Instant.now().plus(expiresInMins, ChronoUnit.MINUTES)
//        cache[to] = Entry(otp, expiry)
//
//        Message.creator(
//            PhoneNumber(to),
//            PhoneNumber(shortCode),
//            "Your verification code is: $otp"
//        ).create()
//    }
//
//    fun verify(to: String, submitted: String): Boolean {
//        val entry = cache[to] ?: return false
//        return if (Instant.now().isBefore(entry.expiresAt) && entry.code == submitted) {
//            cache.remove(to)
//            true
//        } else {
//            cache.remove(to)
//            false
//        }
//    }
//}