package app.dataservice

import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import app.dataservice.boundaries.VehicleBoundary
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Activates test-specific application properties
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows @BeforeAll/@AfterAll to be non-static
class VehicleControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    // Will hold the JWT token used for authentication in test requests
    lateinit var jwtToken: String

    // Will store the ID of the created vehicle for use in tests
    lateinit var createdVehicleId: String

    // ------------------ Utility ------------------

    // Creates a valid VehicleBoundary object for use in tests
    fun createTestVehicleBoundary(
        type: String = "Car",
        description: String = "Test Car",
        color: String = "Red",
        imageUrl: String = "http://example.com/car.jpg",
        latitude: Float = 32.0f,
        longitude: Float = 34.0f,
        cameraId: String = "cam123",
        manufacturer: String = "Toyota"
    ): VehicleBoundary {
        return VehicleBoundary(
            id = null,
            cameraId = cameraId,
            type = type,
            manufacturer = manufacturer,
            color = color,
            typeProb = null,
            manufacturerProb = null,
            colorProb = null,
            imageUrl = imageUrl,
            description = description,
            timestamp = null,
            stayDuration = 0,
            stayDurationFormatted = null,
            top = null,
            left = null,
            width = null,
            height = null,
            latitude = latitude,
            longitude = longitude
        )
    }

    // ------------------ Setup ------------------

    @BeforeAll
    fun loginAndSetup() {
        // 1. Log in and get a JWT token
        val loginRequest = LoginBoundary(email = "admin@gmail.com", password = "admin")

        println("Logging in with credentials: $loginRequest")

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

        // 2. Clean up: Delete all vehicles before starting tests
        webTestClient.delete()
            .uri("/vehicles/deleteAllVehicles")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk

        // 3. Create a single vehicle to use in tests
        val vehicle = createTestVehicleBoundary()

        createdVehicleId = webTestClient.post()
            .uri("/vehicles/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(vehicle)
            .exchange()
            .expectStatus().isOk
            .expectBody(VehicleBoundary::class.java)
            .returnResult()
            .responseBody!!
            .id!!
    }

    // ------------------ Tests ------------------

    @Test
    fun `test get vehicle by id - success`() {
        // 1. Act: Send GET request to fetch a vehicle by ID
        webTestClient.get()
            .uri("/vehicles/getVehicleById/$createdVehicleId")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Expect 200 OK and correct vehicle ID in response
            .expectStatus().isOk
            .expectBody(VehicleBoundary::class.java)
            .consumeWith {
                Assertions.assertEquals(createdVehicleId, it.responseBody!!.id)
            }
    }

    @Test
    fun `test update vehicle - success`() {
        // 1. Arrange: Create an updated vehicle object
        val updatedVehicle = createTestVehicleBoundary(
            imageUrl = "http://example.com/newimage.jpg",
            latitude = 33.0f,
            longitude = 35.0f,
            cameraId = "cam456"
        )

        // 2. Act: Send PUT request to update vehicle
        webTestClient.put()
            .uri("/vehicles/update/$createdVehicleId")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatedVehicle)
            .exchange()
            // 3. Assert: Expect updated image URL in response
            .expectStatus().isOk
            .expectBody(VehicleBoundary::class.java)
            .consumeWith {
                val vehicle = it.responseBody!!
                Assertions.assertEquals("http://example.com/newimage.jpg", vehicle.imageUrl)
            }
    }

    @Test
    fun `test create vehicle - missing fields - bad request`() {
        // 1. Arrange: Create a vehicle with missing required fields
        val badVehicle = createTestVehicleBoundary(
            type = "", description = "", color = "", imageUrl = "",
            latitude = 0f, longitude = 0f, cameraId = ""
        )

        // 2. Act & Assert: Expect 400 Bad Requests on invalid input
        webTestClient.post()
            .uri("/vehicles/create")
            .header("Authorization", "Bearer $jwtToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(badVehicle)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `test get vehicles page - success`() {
        // 1. Act: Request a paginated list of vehicles
        webTestClient.get()
            .uri("/vehicles/getVehicles?page=0&size=5")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            // 2. Assert: Response is OK and the content type is JSON
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBodyList(VehicleBoundary::class.java)
    }

    @Test
    fun `test get vehicles by timestamp - invalid format`() {
        // 1. Act & Assert: Expect 400 Bad Request for invalid timestamp
        webTestClient.get()
            .uri("/vehicles/getVehiclesByTimestamp/invalid-timestamp")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isBadRequest
    }

    // ------------------ Cleanup ------------------

    @AfterAll
    fun cleanup() {
        // Cleanup: Delete all vehicles after all tests completed
        webTestClient.delete()
            .uri("/vehicles/deleteAllVehicles")
            .header("Authorization", "Bearer $jwtToken")
            .exchange()
            .expectStatus().isOk
    }
}