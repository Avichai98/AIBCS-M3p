package app.alertservice

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.boundaries.VehicleBoundary
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

	lateinit var jwtToken: String

	// ------------------ Utility ------------------

	// Creates a valid VehicleBoundary object for use inside AlertBoundary
	fun createTestVehicleBoundary(): VehicleBoundary {
		return VehicleBoundary(
			id = null,
			cameraId = "testCamId",
			type = "Car",
			manufacturer = "Toyota",
			color = "Blue",
			typeProb = null,
			manufacturerProb = null,
			colorProb = null,
			imageUrl = "http://example.com/car.jpg",
			description = "Test vehicle",
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

	// Creates a valid AlertBoundary object for testing alert creation
	fun createTestAlertBoundary(): AlertBoundary {
		return AlertBoundary(
			id = null,
			cameraId = "testCamId",
			type = "Parking",
			severity = "Low",
			description = "Test alert description",
			timestamp = LocalDateTime.now(),
			vehicleBoundary = createTestVehicleBoundary()
		)
	}

	// ------------------ Setup ------------------

	@BeforeAll
	fun loginAsAdmin() {
		// 1. Login as admin to get JWT token for authentication
		val loginRequest = mapOf("email" to "admin@gmail.com", "password" to "admin")

		jwtToken = webTestClient.post()
			.uri("http://localhost:8080/users/login") // Assumes user service runs on port 8080
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(loginRequest)
			.exchange()
			.expectStatus().isOk
			.expectBody(Map::class.java)
			.returnResult()
			.responseBody!!["token"] as String
	}

	// ------------------ Tests ------------------

	@Test
	fun `test create alert - success`() {
		// Arrange: Prepare a valid AlertBoundary
		val alert = createTestAlertBoundary()

		// Act: Send POST request to /alerts/create
		webTestClient.post()
			.uri("/alerts/create")
			.header("Authorization", "Bearer $jwtToken")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(alert)
			.exchange()
			// Assert: Expect 200 OK and non-null response body
			.expectStatus().isOk
			.expectBody(AlertBoundary::class.java)
			.consumeWith {
				Assertions.assertNotNull(it.responseBody!!.id)
			}
	}

	@Test
	fun `test create alert - missing fields - bad request`() {
		// Arrange: Create an invalid alert with missing required fields
		val badAlert = AlertBoundary(
			id = null,
			cameraId = "",
			type = "",
			severity = "",
			description = "",
			timestamp = null,
			vehicleBoundary = null
		)

		// Act & Assert: Send POST and expect 400 Bad Request
		webTestClient.post()
			.uri("/alerts/create")
			.header("Authorization", "Bearer $jwtToken")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(badAlert)
			.exchange()
			.expectStatus().isBadRequest
	}
}
