// src/main/kotlin/com/example/sms/clicksend/ClickSendModels.kt
package com.example.runitup.clicksend

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClickSendSmsMessage(
    val source: String = "java",
    val body: String,
    val to: String,
    val from: String? = null,    // optional, use approved sender ID where supported
    val schedule: Long? = null,  // unix timestamp (seconds) for scheduled send
    val custom_string: String? = null,
)

data class ClickSendSmsRequest(
    val messages: List<ClickSendSmsMessage>
)

data class ClickSendResponse(
    val http_code: Int?,
    val response_code: String?,
    val response_msg: String?,
    val data: Any? = null
)
