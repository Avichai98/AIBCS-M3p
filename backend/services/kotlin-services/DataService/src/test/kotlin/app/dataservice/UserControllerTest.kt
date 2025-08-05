package app.dataservice

import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import app.dataservice.boundaries.UserBoundary
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "secret") // Activates test-specific application properties
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows @BeforeAll/@AfterAll to be non-static
class UserControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    lateinit var adminJwtToken: String
    lateinit var createdUserId: String

    // ------------------ Utility ------------------

    // Creates a valid UserBoundary object for test user creation
    fun createTestUserBoundary(): UserBoundary {
        return UserBoundary(
            id = null,
            firstName = "John",
            lastName = "Doe",
            email = "johndoe@example.com",
            password = "password123",
            mobile = "1234567890",
            username = "johndoe",
            roles = setOf("USER"),
            createdAt = null,
            updatedAt = null
        )
    }

    // ------------------ Setup ------------------

    @BeforeAll
    fun setup() {
        // 1. Login as admin to get JWT token
        val loginRequest = LoginBoundary(email = "admin@gmail.com", password = "admin")

        adminJwtToken = webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody!!
            .token ?: throw IllegalStateException("Failed to obtain JWT token")

        // 2. Delete all users except admin
        val existingUsers = webTestClient.get()
            .uri("/users/getUsers?page=0&size=1000")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(UserBoundary::class.java)
            .returnResult()
            .responseBody!!

        for (user in existingUsers) {
            if (user.email != "admin@gmail.com") {
                webTestClient.delete()
                    .uri("/users/delete/${user.id}")
                    .header("Authorization", "Bearer $adminJwtToken")
                    .exchange()
                    .expectStatus().isOk
            }
        }

        // 3. Create a user for test cases
        val user = createTestUserBoundary()

        createdUserId = webTestClient.post()
            .uri("/users/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!
    }

    // ------------------ Tests ------------------

    @Test
    fun `test get user by id - success`() {
        // Act: Get user by ID
        webTestClient.get()
            .uri("/users/getUserById/$createdUserId")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            // Assert: Expect 200 OK and correct user ID
            .expectStatus().isOk
            .expectBody(UserBoundary::class.java)
            .consumeWith {
                Assertions.assertEquals(createdUserId, it.responseBody!!.id)
            }
    }

    @Test
    fun `test get user by email - success`() {
        // Act: Get user by email
        webTestClient.get()
            .uri("/users/getUserByEmail/johndoe@example.com")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            // Assert: Expect 200 OK and correct email
            .expectStatus().isOk
            .expectBody(UserBoundary::class.java)
            .consumeWith {
                Assertions.assertEquals("johndoe@example.com", it.responseBody!!.email)
            }
    }

    @Test
    fun `test login - success`() {
        // Act: Perform login with created user credentials
        val loginRequest = LoginBoundary(email = "johndoe@example.com", password = "password123")

        webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            // Assert: Expect 200 OK and non-null token
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody!!.token)
            }
    }

    @Test
    fun `test login - invalid password`() {
        // Act & Assert: Login with the wrong password should fail
        val loginRequest = LoginBoundary(email = "johndoe@example.com", password = "wrongpass")

        webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `test update user - success`() {
        // Arrange: Update username for existing user
        val updatedUser = UserBoundary(
            id = null,
            firstName = "John",
            lastName = "Doe",
            email = null,
            password = null,
            mobile = null,
            username = "johnupdated",
            roles = emptySet(),
            createdAt = null,
            updatedAt = null
        )

        // Act: Send PUT request
        webTestClient.put()
            .uri("/users/update/$createdUserId")
            .header("Authorization", "Bearer $adminJwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedUser)
            .exchange()
            // Assert: Expect 200 OK (no content)
            .expectStatus().isOk
    }

    @Test
    fun `test get all users - pagination`() {
        // Act: Request a user list with pagination
        webTestClient.get()
            .uri("/users/getUsers?page=0&size=5")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            // Assert: Expect 200 OK with text/event-stream response
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
    }

    @Test
    fun `test delete user - success`() {
        // Arrange: Create a new user to delete
        val userToDelete = createTestUserBoundary().apply { email = "delete@example.com"; username = "deleteuser" }

        val userIdToDelete = webTestClient.post()
            .uri("/users/create")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userToDelete)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserBoundary::class.java)
            .returnResult()
            .responseBody!!.id!!

        // Act: Delete user by ID
        webTestClient.delete()
            .uri("/users/delete/$userIdToDelete")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            // Assert: Expect 200 OK
            .expectStatus().isOk
    }

    // ------------------ Cleanup ------------------

    @AfterAll
    fun cleanup() {
        // Delete all users except admin after tests
        val users = webTestClient.get()
            .uri("/users/getUsers?page=0&size=1000")
            .header("Authorization", "Bearer $adminJwtToken")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(UserBoundary::class.java)
            .returnResult()
            .responseBody!!

        for (user in users) {
            if (user.email != "admin@gmail.com") {
                webTestClient.delete()
                    .uri("/users/delete/${user.id}")
                    .header("Authorization", "Bearer $adminJwtToken")
                    .exchange()
                    .expectStatus().isOk
            }
        }
    }
}