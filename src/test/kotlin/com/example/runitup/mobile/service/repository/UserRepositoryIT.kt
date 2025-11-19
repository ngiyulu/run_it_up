// src/test/kotlin/com/example/runitup/mobile/repository/UserRepositoryIT.kt
package com.example.runitup.mobile.service.repository

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Testcontainers
@DataMongoTest
class UserRepositoryIT @Autowired constructor(
    private val userRepository: UserRepository
) {

    companion object {
        @Container
        val mongo: MongoDBContainer = MongoDBContainer("mongo:7.0.5")

        @JvmStatic
        @DynamicPropertySource
        fun mongoProps(registry: DynamicPropertyRegistry) {
            // Make Spring Boot Data Mongo use the Testcontainers Mongo instance
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }

    @BeforeEach
    fun cleanDb() {
        userRepository.deleteAll()
    }

    /**
     * Helper to build a User with the fields we need for the queries.
     *
     * 👉 IMPORTANT:
     * Adjust this to match your real `User` constructor and required fields.
     */
    private fun newUser(
        id: String? = null,
        email: String = "user@example.com",
        phoneNumber: String = "+15550000000",
        auth: String = "",
        verifiedPhone: Boolean = false,
        linkedAdmin: String? = null,
        stripeId: String? = null,
        createdAt: Instant = Instant.now(),
        loggedInAt: Long = 0
    ): User {
        val user = User(
            id = id,
            email = email,
            phoneNumber = phoneNumber,
            auth = auth,
            verifiedPhone = verifiedPhone,
            linkedAdmin = linkedAdmin,
            stripeId = stripeId,
            // 🔧 add / adjust any other required ctor args here
        )

        // If your User extends BaseModel with these fields as vars:
        user.createdAt = createdAt
        user.loggedInAt = loggedInAt

        return user
    }

    // -------------------------------------------------------------------------
    // findByIdentifier (custom @Query on id)
    // -------------------------------------------------------------------------

    @Test
    fun `findByIdentifier returns user with matching id`() {
        val saved = userRepository.save(
            newUser(
                id = null,
                email = "id@test.com",
                phoneNumber = "+15551111111"
            )
        )

        val found = userRepository.findByIdentifier(saved.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(saved.id)
        assertThat(found.email).isEqualTo("id@test.com")
    }

    // -------------------------------------------------------------------------
    // findByEmail (custom @Query on email)
    // -------------------------------------------------------------------------

    @Test
    fun `findByEmail returns user with matching email`() {
        val u1 = newUser(email = "a@test.com")
        val u2 = newUser(email = "b@test.com")
        userRepository.saveAll(listOf(u1, u2))

        val found = userRepository.findByEmail("b@test.com")

        assertThat(found).isNotNull
        assertThat(found!!.email).isEqualTo("b@test.com")
    }

    // -------------------------------------------------------------------------
    // findByPhone (custom @Query on phoneNumber)
    // -------------------------------------------------------------------------

    @Test
    fun `findByPhone returns user with matching phone number`() {
        val u1 = newUser(phoneNumber = "+15550000001")
        val u2 = newUser(phoneNumber = "+15550000002")
        userRepository.saveAll(listOf(u1, u2))

        val found = userRepository.findByPhone("+15550000002")

        assertThat(found).isNotNull
        assertThat(found!!.phoneNumber).isEqualTo("+15550000002")
    }

    // -------------------------------------------------------------------------
    // findByAuth (custom @Query on auth)
    // -------------------------------------------------------------------------

    @Test
    fun `findByAuth returns user with matching auth`() {
        val u1 = newUser(auth = "auth-1")
        val u2 = newUser(auth = "auth-2")
        userRepository.saveAll(listOf(u1, u2))

        val found = userRepository.findByAuth("auth-2")

        assertThat(found).isNotNull
        assertThat(found!!.auth).isEqualTo("auth-2")
    }

    // -------------------------------------------------------------------------
    // findAllByVerifiedPhoneOrderByCreatedAtDesc
    // -------------------------------------------------------------------------

    @Test
    fun `findAllByVerifiedPhoneOrderByCreatedAtDesc returns only verified users sorted by createdAt desc`() {
        val older = Instant.now().minusSeconds(3600)
        val newer = Instant.now()

        val u1 = newUser(
            email = "v1@test.com",
            verifiedPhone = true,
            createdAt = older
        )
        val u2 = newUser(
            email = "v2@test.com",
            verifiedPhone = true,
            createdAt = newer
        )
        val u3 = newUser(
            email = "nv@test.com",
            verifiedPhone = false,
            createdAt = newer
        )

        userRepository.saveAll(listOf(u1, u2, u3))

        val page = userRepository.findAllByVerifiedPhoneOrderByCreatedAtDesc(
            verifiedPhone = true,
            pageable = PageRequest.of(0, 10)
        )

        val emails = page.content.map { it.email }

        assertThat(emails)
            .containsExactly("v2@test.com", "v1@test.com")  // newest first
            .doesNotContain("nv@test.com")
    }

    // -------------------------------------------------------------------------
    // findAllByLinkedAdminOrderByCreatedAtDesc
    // -------------------------------------------------------------------------

    @Test
    fun `findAllByLinkedAdminOrderByCreatedAtDesc filters by linkedAdmin and sorts desc`() {
        val linked = "admin-123"

        val oldUser = newUser(
            email = "old@test.com",
            linkedAdmin = linked,
            createdAt = Instant.now().minusSeconds(3600)
        )
        val newUserForAdmin = newUser(
            email = "new@test.com",
            linkedAdmin = linked,
            createdAt = Instant.now()
        )
        val otherAdminUser = newUser(
            email = "other@test.com",
            linkedAdmin = "admin-999",
            createdAt = Instant.now()
        )

        userRepository.saveAll(listOf(oldUser, newUserForAdmin, otherAdminUser))

        val page = userRepository.findAllByLinkedAdminOrderByCreatedAtDesc(
            linkedAdmin = linked,
            pageable = PageRequest.of(0, 10)
        )

        val emails = page.content.map { it.email }

        assertThat(emails)
            .containsExactly("new@test.com", "old@test.com")
            .doesNotContain("other@test.com")
    }

    // -------------------------------------------------------------------------
    // findByStripeId
    // -------------------------------------------------------------------------

    @Test
    fun `findByStripeId returns user with matching stripeId`() {
        val u1 = newUser(stripeId = "cus_123")
        val u2 = newUser(stripeId = "cus_456")
        userRepository.saveAll(listOf(u1, u2))

        val found = userRepository.findByStripeId("cus_456")

        assertThat(found).isNotNull
        assertThat(found!!.stripeId).isEqualTo("cus_456")
    }

    // -------------------------------------------------------------------------
    // findByEmailIn & findByPhoneNumberIn
    // -------------------------------------------------------------------------

    @Test
    fun `findByEmailIn returns all users with emails in list`() {
        val u1 = newUser(email = "a@test.com")
        val u2 = newUser(email = "b@test.com")
        val u3 = newUser(email = "c@test.com")
        userRepository.saveAll(listOf(u1, u2, u3))

        val result = userRepository.findByEmailIn(listOf("a@test.com", "c@test.com"))

        val emails = result.map { it.email }
        assertThat(emails)
            .hasSize(2)
            .containsExactlyInAnyOrder("a@test.com", "c@test.com")
    }

    @Test
    fun `findByPhoneNumberIn returns all users with phone numbers in list`() {
        val u1 = newUser(phoneNumber = "+15550000001")
        val u2 = newUser(phoneNumber = "+15550000002")
        val u3 = newUser(phoneNumber = "+15550000003")
        userRepository.saveAll(listOf(u1, u2, u3))

        val result = userRepository.findByPhoneNumberIn(
            listOf("+15550000001", "+15550000003")
        )

        val phones = result.map { it.phoneNumber }
        assertThat(phones)
            .hasSize(2)
            .containsExactlyInAnyOrder("+15550000001", "+15550000003")
    }

    // -------------------------------------------------------------------------
    // findAllByOrderByLoggedInAtDesc
    // -------------------------------------------------------------------------

    @Test
    fun `findAllByOrderByLoggedInAtDesc returns users sorted by loggedInAt desc`() {
        val now = Instant.now()
        val earlier = now.minusSeconds(3600)

        val u1 = newUser(
            email = "u1@test.com",
            loggedInAt = earlier.epochSecond
        )
        val u2 = newUser(
            email = "u2@test.com",
            loggedInAt = earlier.epochSecond
        )
        val u3 = newUser(
            email = "u3@test.com",
            loggedInAt = 0
        )

        userRepository.saveAll(listOf(u1, u2, u3))

        val page = userRepository.findAllByOrderByLoggedInAtDesc(
            pageable = PageRequest.of(0, 10)
        )

        val emails = page.content.map { it.email }

        // Depending on how nulls are handled by Mongo sorting,
        // usually docs with null loggedInAt come last.
        assertThat(emails.first()).isEqualTo("u1@test.com")
        assertThat(emails[1]).isEqualTo("u2@test.com")
    }
}
