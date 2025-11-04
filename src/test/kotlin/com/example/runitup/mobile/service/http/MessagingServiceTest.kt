@file:Suppress("MemberVisibilityCanBePrivate")

package com.example.runitup.mobile.service.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateParticipantModel
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.DeleteParticipantFromConversationModel
import model.messaging.Conversation
import model.messaging.MessagingUser
import model.messaging.Participant
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessagingServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var svc: MessagingService

    private val om = jacksonObjectMapper()

    @BeforeAll
    fun setupAll() {
        server = MockWebServer()
        server.start()

        val webClient = WebClient.builder()
            .baseUrl(server.url("/").toString().removeSuffix("/"))
            .build()

        svc = MessagingService()
        injectClient(svc, webClient)
    }

    @AfterAll
    fun teardownAll() {
        server.shutdown()
    }

    // ---------- createUser ----------

    @Test
    @DisplayName("createUser: 200 -> ok with body")
    fun createUser_ok() {
        // Return only the fields we assert on
        val body = mapOf(
            "id" to "u1",
            "firstName" to "Jane",
            "lastName" to "Doe",
            "email" to "jane@example.com"
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(om.writeValueAsString(body))
        )

        val req = MessagingUser(id = "uX", firstName = "X", lastName = "Y", email = "x@y.com")

        StepVerifier.create(svc.createUser(req))
            .assertNext { res ->
                Assertions.assertTrue(res.ok)
                Assertions.assertNotNull(res.data)
                Assertions.assertNull(res.error)
                Assertions.assertEquals("u1", res.data!!.id)
                Assertions.assertEquals("jane@example.com", res.data!!.email)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("createUser: 502 -> err with mapped body")
    fun createUser_error() {
        server.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("downstream is mad")
        )

        val req = MessagingUser(id = "uErr", firstName = "E", lastName = "R", email = "e@r.com")

        StepVerifier.create(svc.createUser(req))
            .assertNext { res ->
                Assertions.assertFalse(res.ok)
                Assertions.assertNull(res.data)
                Assertions.assertNotNull(res.error)
                Assertions.assertEquals(502, res.error!!.status)
                Assertions.assertEquals("service-a", res.error!!.source)
                Assertions.assertTrue(res.error!!.message.contains("Downstream returned 502"))
                Assertions.assertEquals("downstream is mad", res.error!!.body)
            }
            .verifyComplete()
    }

    // ---------- createConversation ----------

    @Test
    @DisplayName("createConversation: 200 -> ok with body")
    fun createConversation_ok() {
        // Minimal JSON the service can map to Conversation (or via ObjectMapper) for the fields we assert
        val convoJson = mapOf("id" to "c1", "title" to "Run #1")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(om.writeValueAsString(convoJson))
        )

        val convo = Conversation(id = "c1", title = "Run #1") // used only for request payload shape
        val req = CreateConversationModel(runSessionId = "rs1", conversation = convo)

        StepVerifier.create(svc.createConversation(req))
            .assertNext { res ->
                Assertions.assertTrue(res.ok)
                Assertions.assertEquals("c1", res.data!!.id)
                Assertions.assertEquals("Run #1", res.data!!.title)
                Assertions.assertNull(res.error)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("createConversation: 500 -> err maps status/body")
    fun createConversation_error() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"boom\"}")
        )

        val convo = Conversation(id = "cX", title = "X")
        val req = CreateConversationModel(runSessionId = "rsX", conversation = convo)

        StepVerifier.create(svc.createConversation(req))
            .assertNext { res ->
                Assertions.assertFalse(res.ok)
                val err = requireNotNull(res.error)
                Assertions.assertEquals(500, err.status)
                Assertions.assertEquals("service-a", err.source)
                Assertions.assertTrue(err.message.contains("Downstream returned 500"))
                Assertions.assertEquals("{\"error\":\"boom\"}", err.body)
            }
            .verifyComplete()
    }

    // ---------- createParticipant ----------

    @Test
    @DisplayName("createParticipant: 200 -> ok with body")
    fun createParticipant_ok() {
        // Return a minimal participant JSON with the field we assert
        val participantJson = mapOf("userId" to "u1")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(om.writeValueAsString(participantJson))
        )

        val participant = Participant() // request payload can be minimal
        val req = CreateParticipantModel(
            conversationId = "c1",
            participantDoc = participant,
            conversationTitle = "Run #1"
        )

        StepVerifier.create(svc.createParticipant(req))
            .assertNext { res ->
                Assertions.assertTrue(res.ok)
                Assertions.assertEquals("u1", res.data!!.userId)
                Assertions.assertNull(res.error)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("createParticipant: 404 -> err maps status/body")
    fun createParticipant_error() {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("not found")
        )

        val participant = Participant()
        val req = CreateParticipantModel(conversationId = "cX", participantDoc = participant, conversationTitle = "X")

        StepVerifier.create(svc.createParticipant(req))
            .assertNext { res ->
                Assertions.assertFalse(res.ok)
                val err = requireNotNull(res.error)
                Assertions.assertEquals(404, err.status)
                Assertions.assertEquals("service-a", err.source)
                Assertions.assertTrue(err.message.contains("Downstream returned 404"))
                Assertions.assertEquals("not found", err.body)
            }
            .verifyComplete()
    }

    // ---------- removeParticipant ----------

    @Test
    @DisplayName("removeParticipant: 200 -> ok(Unit)")
    fun removeParticipant_ok() {
        // Return a minimal JSON body so any bodyToMono(...) succeeds
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )

        val req = DeleteParticipantFromConversationModel(
            "userId",
            "conversationId"
        )

        StepVerifier.create(svc.removeParticipant(req))
            .assertNext { res ->
                Assertions.assertTrue(res.ok)
                Assertions.assertNotNull(res.data) // Unit or any non-null placeholder
                Assertions.assertNull(res.error)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("removeParticipant: 500 -> err maps status/body")
    fun removeParticipant_error() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody("oops")
        )

        val req = DeleteParticipantFromConversationModel("userId", "conversation")

        StepVerifier.create(svc.removeParticipant(req))
            .assertNext { res ->
                Assertions.assertFalse(res.ok)
                val err = requireNotNull(res.error)
                Assertions.assertEquals(500, err.status)
                Assertions.assertEquals("service-a", err.source)
                Assertions.assertTrue(err.message.contains("Downstream returned 500"))
                Assertions.assertEquals("oops", err.body)
            }
            .verifyComplete()
    }

    // ---------- helpers ----------

    private fun injectClient(target: MessagingService, webClient: WebClient) {
        val f = MessagingService::class.java.getDeclaredField("client")
        f.isAccessible = true
        f.set(target, webClient)
    }
}
