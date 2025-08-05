package app.dataservice

import app.dataservice.boundaries.AlertBoundary
import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import app.dataservice.boundaries.VehicleBoundary
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "secret") // Activates test-specific application properties
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows @BeforeAll/@AfterAll to be non-static
class AlertControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    // Will hold the JWT token used for authentication in test requests
    lateinit var jwtToken: String

    // Will store the ID of the created alert for use in tests
    lateinit var createdAlertId: String

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

    // Creates a valid VehicleBoundary object for use inside AlertBoundary
    fun createTestVehicleBoundary(): VehicleBoundary {
        return VehicleBoundary(
            id = null,
            cameraId = createdCameraId,
            type = "Car",
            manufacturer = "Toyota",
            color = "Red",
            typeProb = null,
            manufacturerProb = null,
            colorProb = null,
            imageUrl = "http://example.com/car.jpg",
            description = "Test Vehicle",
            timestamp = null,
            stayDuration = 0,
            stayDurationFormatted = null,
            top = null,
            left = null,
            width = null,
            height = null,
            latitude = 32.0f,
            longitude = 34.0f
        )
    }

    // Creates a valid AlertBoundary object for use in tests
    fun createTestAlertBoundary(): AlertBoundary {
        return AlertBoundary(
            id = null,
            cameraId = createdCameraId,
            type = "Speeding",
            severity = "High",
            description = "Vehicle exceeded speed limit",
            timestamp = LocalDateTime.now(),
            vehicleBoundary = createTestVehicleBoundary()
        )
    }

    // ------------------ Setup ------------------

    @BeforeAll
    fun loginAndSetup() {
        // 1. Log in and get JWT token
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

        // 2. Create a camera before creating alerts
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

        // 3. Clean up: Delete all alerts before starting tests
        webTestClient.delete()
            .uri("/alerts/deleteAllAlerts")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk

        // 4. Create alert to use in tests
        val alert = createTestAlertBoundary()

        createdAlertId = webTestClient.post()
            .uri("/alerts/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(alert)
            .exchange()
            .expectStatus().isOk
            .expectBody(AlertBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!
    }

    // ------------------ Tests ------------------

    @Test
    fun `test get alert by id - success`() {
        // 1. Act: Send GET request to fetch an alert by ID
        webTestClient.get()
            .uri("/alerts/getAlertById/$createdAlertId")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Expect 200 OK and correct alert ID in response
            .expectStatus().isOk
            .expectBody(AlertBoundary::class.java)
            .consumeWith {
                Assertions.assertEquals(createdAlertId, it.responseBody!!.id)
            }
    }

    @Test
    fun `test create alert - missing fields - bad request`() {
        // 1. Arrange: Create an alert with missing required fields
        val badAlert = AlertBoundary(
            id = null,
            cameraId = "",
            type = "",
            severity = "",
            description = "",
            timestamp = null,
            vehicleBoundary = null
        )

        // 2. Act & Assert: Expect 400 Bad Request on invalid input
        webTestClient.post()
            .uri("/alerts/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(badAlert)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `test get alerts page - success`() {
        // 1. Act: Request a paginated list of alerts
        webTestClient.get()
            .uri("/alerts/getAlerts?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Response is OK and the content type is text/event-stream
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectBodyList(AlertBoundary::class.java)
    }

    @Test
    fun `test get alerts by vehicle - success`() {
        // 1. Act: Fetch alerts for a given vehicle ID (could be null - just test the endpoint)
        webTestClient.get()
            .uri("/alerts/getAlertsByVehicle/${createTestVehicleBoundary().id}?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Response is OK (empty list is acceptable)
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
    }

    @Test
    fun `test get alerts by timestamp - invalid format`() {
        // 1. Act & Assert: Expect 400 Bad Request for invalid timestamp format
        webTestClient.get()
            .uri("/alerts/getAlertsByTimestamp/invalid-timestamp?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `test get alerts by camera - success`() {
        // 1. Act: Fetch alerts for a given camera ID
        webTestClient.get()
            .uri("/alerts/getAlertsByCamera/cam123?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Response is OK and content type is application/json
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
    }

    @Test
    fun `test delete alert - success`() {
        // 1. Arrange: Create a new alert to be deleted
        val alert = createTestAlertBoundary()

        val alertIdToDelete = webTestClient.post()
            .uri("/alerts/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(alert)
            .exchange()
            .expectStatus().isOk
            .expectBody(AlertBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!

        // 2. Act: Send DELETE request to remove the alert
        webTestClient.delete()
            .uri("/alerts/delete/$alertIdToDelete")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 3. Assert: Expect 200 OK (or no content depending on your implementation)
            .expectStatus().isOk
    }

    // ------------------ Cleanup ------------------

    @AfterAll
    fun cleanup() {
        // Cleanup: Delete all alerts after all tests completed
        webTestClient.delete()
            .uri("/alerts/deleteAllAlerts")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk
    }
}
