package  com.example.runitup.service
import com.example.runitup.constants.AppConstant.apnsPushGateway
import com.example.runitup.constants.AppConstant.fcmPushGateway
import com.example.runitup.enum.PhoneType
import com.example.runitup.model.Phone
import com.example.runitup.push.PushGateway
import com.example.runitup.repository.PhoneRepository
import com.example.runitup.web.rest.v1.dto.PushNotification
import com.example.runitup.web.rest.v1.dto.PushResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushService{

    @Autowired
    lateinit var phoneRepo: PhoneRepository

    @Autowired
    @Qualifier(fcmPushGateway)
    lateinit var fcm: PushGateway

    @Autowired
    @Qualifier(apnsPushGateway)
    lateinit var apns: PushGateway


    // this determines is we want to use firebase tos end to ios or fcm
    // android will always  use fcm
    @Value("\${push.useApnsDirect}")
    var useApnsDirect: Boolean = false
    @Transactional(readOnly = true)
    fun sendToPhone(phone: Phone, notif: PushNotification): PushResult {
        val gateway = when (phone.type) {
            PhoneType.ANDROID -> fcm
            PhoneType.IOS -> if (useApnsDirect) apns else fcm
        }
        val result = gateway.sendToTokens(listOf(phone.token), notif)
        handleInvalidTokens(result)
        return result
    }

    @Transactional(readOnly = true)
    fun sendToPhones(phones: List<Phone>, notif: PushNotification): PushResult {
        val (androidTokens, iosTokens) = phones.partition { it.type == PhoneType.ANDROID }
            .let { pair -> pair.first.map { it.token } to pair.second.map { it.token } }

        var requested = 0; var success = 0; var failed = 0
        val invalid = mutableListOf<String>(); val errors = mutableListOf<String>()

        if (androidTokens.isNotEmpty()) {
            val r = fcm.sendToTokens(androidTokens, notif)
            requested += r.requested; success += r.success; failed += r.failed
            invalid += r.invalidTokens; errors += r.errors
        }
        if (iosTokens.isNotEmpty()) {
            val gw = if (useApnsDirect) apns else fcm
            val r = gw.sendToTokens(listOf("5cd69584eb0b0f0fcaaa12c96c0ee6ad4ff0ec90d155374065e00ff217ac6e8d"), notif)
            requested += r.requested; success += r.success; failed += r.failed
            invalid += r.invalidTokens; errors += r.errors
        }

        val result = PushResult(requested, success, failed, invalid, errors)
        handleInvalidTokens(result)
        return result
    }

    private fun handleInvalidTokens(result: PushResult) {
        //TODO: we need to delete invalid tokens
        if (result.invalidTokens.isNotEmpty()) {
//            // Soft-delete or clear tokens so you stop retrying dead devices
//            val dead = phoneRepo.findAllByTokenIn(result.invalidTokens)
//            dead.forEach { it.token = "" }
//            phoneRepo.saveAll(dead)
        }
    }
}
