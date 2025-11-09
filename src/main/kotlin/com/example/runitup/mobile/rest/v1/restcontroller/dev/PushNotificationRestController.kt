package com.example.runitup.mobile.rest.v1.restcontroller.dev


import com.example.runitup.mobile.repository.service.PhoneDbService
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult
import com.example.runitup.mobile.service.PushService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles
@RestController
@RequestMapping("/api/push")
class PushNotificationRestController {

    @Autowired
    lateinit var phoneRepository: PhoneDbService

    @Autowired
    lateinit var pushService: PushService
    @PostMapping("/send")
    fun send(@RequestBody req: PushTestData): PushResult {
        val notification = PushNotification(
            title = req.title,
            body = req.body,
            data = req.data,
            clickAction = req.clickAction,
            badge = req.badge
        )
        val phones = phoneRepository.findAllByPhoneId(req.phoneId)
        return pushService.sendToPhonesAudited(phones, notification, "", null, "", null)
    }
}

data class PushTestData(val title: String, val body: String, val phoneId: String, val clickAction:String?, val badge: Int?,  val data: Map<String, String> = emptyMap())