package app.dataservice

import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.CameraSchedule
import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "secret") // Activates test-specific application properties
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows @BeforeAll/@AfterAll to be non-static
class CameraControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    // Will hold the JWT token used for authentication in test requests
    lateinit var jwtToken: String

    // Will store the ID of the created camera for use in tests
    lateinit var createdCameraId: String

    // ------------------ Utility ------------------

    // Creates a valid CameraBoundary object for use in tests
    fun createTestCameraBoundary(): CameraBoundary {
        return CameraBoundary(
            id = null,
            name = "Test Camera",
            emails = listOf("admin@gmail.com"),
            location = "Test Location",
            alertCount = 0,
            isActive = true,
            status = "Online",
            lastActivity = "2025-08-01T12:00:00",
            schedule = null
        )
    }

    // Creates a valid CameraSchedule object for scheduling tests
    fun createTestSchedule(): CameraSchedule {
        return CameraSchedule(
            enabled = true,
            days = listOf("Monday", "Wednesday"),
            startTime = "08:00",
            endTime = "18:00"
        )
    }

    // ------------------ Setup ------------------

    @BeforeAll
    fun loginAndSetup() {
        // 1. Log in and get a JWT token
        val loginRequest = LoginBoundary(email = "admin@gmail.com", password = "admin")

        jwtToken = webTestClient.post()
            .uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody!!
            .token ?: throw IllegalStateException("Failed to obtain JWT token")

        // 2. Delete all existing cameras to ensure a clean slate
        webTestClient.delete()
            .uri("/cameras/deleteAllCameras")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk

        // 3. Create a camera for use in tests
        val camera = createTestCameraBoundary()

        createdCameraId = webTestClient.post()
            .uri("/cameras/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(camera)
            .exchange()
            .expectStatus().isOk
            .expectBody(CameraBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!
    }

    // ------------------ Tests ------------------

    @Test
    fun `test get camera by id - success`() {
        // 1. Act: Request the camera by ID
        webTestClient.get()
            .uri("/cameras/getCameraById/$createdCameraId")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Expect 200 OK and correct ID in response
            .expectStatus().isOk
            .expectBody(CameraBoundary::class.java)
            .consumeWith {
                Assertions.assertEquals(createdCameraId, it.responseBody!!.id)
            }
    }

    @Test
    fun `test create camera - invalid email - bad request`() {
        // 1. Arrange: Prepare camera with invalid email
        val badCamera = createTestCameraBoundary().apply {
            emails = listOf("invalid-email")
        }

        // 2. Act & Assert: Expect 400 Bad Requests due to invalid email
        webTestClient.post()
            .uri("/cameras/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(badCamera)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `test update camera schedule - success`() {
        // 1. Arrange: Prepare a valid schedule
        val schedule = createTestSchedule()

        // 2. Act: Send PUT request to update schedule
        webTestClient.put()
            .uri("/cameras/schedule/$createdCameraId")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(schedule)
            .exchange()
            .expectStatus().isOk

        // 3. Assert: Retrieve schedule and verify it was updated
        webTestClient.get()
            .uri("/cameras/schedule/$createdCameraId")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(CameraSchedule::class.java)
            .consumeWith {
                Assertions.assertEquals(schedule.enabled, it.responseBody!!.enabled)
                Assertions.assertEquals(schedule.days, it.responseBody!!.days)
            }
    }

    @Test
    fun `test get cameras page - success`() {
        // 1. Act: Request a paginated list of cameras
        webTestClient.get()
            .uri("/cameras/getCameras?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Expect a valid list of CameraBoundary objects
            .expectStatus().isOk
            .expectBodyList(CameraBoundary::class.java)
    }

    @Test
    fun `test get cameras by email - success`() {
        // 1. Act: Request cameras by email
        webTestClient.get()
            .uri("/cameras/getCamerasByEmail/admin@gmail.com?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Expect 200 OK with valid response
            .expectStatus().isOk
            .expectBodyList(CameraBoundary::class.java)
    }

    @Test
    fun `test delete camera - success`() {
        // 1. Arrange: Create a camera to be deleted
        val camera = createTestCameraBoundary()

        val cameraIdToDelete = webTestClient.post()
            .uri("/cameras/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(camera)
            .exchange()
            .expectStatus().isOk
            .expectBody(CameraBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!

        // 2. Act: Send DELETE request
        webTestClient.delete()
            .uri("/cameras/delete/$cameraIdToDelete")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 3. Assert: Expect success
            .expectStatus().isOk
    }

    @Test
    fun `test update camera - invalid email - bad request`() {
        // 1. Arrange: Create an update object with invalid email
        val badUpdate = CameraBoundary(
            id = null,
            name = "Updated Camera",
            emails = listOf("bademail"),
            location = null,
            alertCount = null,
            isActive = null,
            status = null,
            lastActivity = null,
            schedule = null
        )

        // 2. Act & Assert: Expect 400 Bad Request
        webTestClient.put()
            .uri("/cameras/update/$createdCameraId")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(badUpdate)
            .exchange()
            .expectStatus().isBadRequest
    }

    // ------------------ Cleanup ------------------

    @AfterAll
    fun cleanup() {
        // Cleanup: Delete all cameras after tests
        webTestClient.delete()
            .uri("/cameras/deleteAllCameras")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk
    }
}
