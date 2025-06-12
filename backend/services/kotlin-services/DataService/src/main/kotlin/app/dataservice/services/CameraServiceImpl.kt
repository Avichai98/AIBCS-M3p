package app.dataservice.services

import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.CameraSchedule
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.CameraCrud
import app.dataservice.interfaces.CameraService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CameraServiceImpl(
    val cameraCrud: CameraCrud
) :
    CameraService {
    lateinit var dataServiceUrl: String
    lateinit var webClient: WebClient

    @Value("\${remote.python.service.url: http://car-detection-service:5000}")
    fun setRemoteUrl(url: String) {
        this.dataServiceUrl = url
    }
    override fun createCamera(camera: CameraBoundary): Mono<CameraBoundary> {
        val emails = camera.emails
        if (!emails.isNullOrEmpty()) {
            for (email in emails) {
                if (!isValidEmail(email)) {
                    return Mono.error(BadRequestException400("Invalid email: $email"))
                }
            }
        }

        return Mono.just(camera)
            .flatMap {
                if (camera.name.isNullOrBlank())
                    Mono.error(BadRequestException400("Name is missing"))
                else if (camera.location.isNullOrBlank())
                    Mono.error(BadRequestException400("Location is missing"))
                else {
                    it.id = null

                    Mono.just(camera)
                }
            }
            .map { camera.toEntity() }
            .flatMap {
                it.isActive = false
                it.alertCount = 0
                it.status = "offline"
                it.lastActivity = ""

                it.schedule?.days = emptyList()
                it.schedule?.startTime = "00:00"
                it.schedule?.endTime = "00:00"
                it.schedule?.enabled = false

                cameraCrud.save(it)
            }
            .map { CameraBoundary(it) }
            .log()
    }

    override fun startCamera(): Mono<Void> {
        return webClient
            .post()
            .uri("start")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Void::class.java)
            .doOnSuccess { println("📸 Camera start response: $it") }
            .onErrorResume { e ->
                Mono.error(BadRequestException400("Camera start failed: ${e.message}"))
            }
            .log()
    }

    override fun stopCamera(): Mono<Void> {
        return webClient
            .post()
            .uri("stop")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Void::class.java)
            .doOnSuccess { println("📸 Camera stop response: $it") }
            .onErrorResume { e ->
                Mono.error(BadRequestException400("Camera stop failed: ${e.message}"))
            }
            .log()
    }

    override fun updateCamera(
        id: String,
        camera: CameraBoundary
    ): Mono<Void> {
        val emails = camera.emails
        if (!emails.isNullOrEmpty()) {
            for (email in emails) {
                if (!isValidEmail(email)) {
                    return Mono.error(BadRequestException400("Invalid email: $email"))
                }
            }
        }

        return cameraCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("Camera with the id: $id not found")))
            .flatMap {
                if (!camera.name.isNullOrBlank())
                    it.name = camera.name

                if (!camera.emails.isNullOrEmpty())
                    it.emails = camera.emails

                if (!camera.location.isNullOrBlank())
                    it.location = camera.location

                if (camera.isActive != null)
                    it.isActive = camera.isActive

                if (camera.status != null)
                    it.status = camera.status

                cameraCrud.save(it)
            }
            .then()
            .log()
    }

    override fun updateCameraSchedule(
        id: String,
        schedule: CameraSchedule
    ): Mono<Void> {
        return cameraCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("Camera with id $id not found")))
            .flatMap {
                it.schedule = schedule
                cameraCrud.save(it)
            }
            .then()
            .log()
    }

    override fun updateCameraStatus(
        id: String,
        status: Boolean
    ): Mono<Void> {
        return cameraCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("Camera with id $id not found")))
            .flatMap {
                it.isActive = status
                cameraCrud.save(it)
            }
            .then()
            .log()
    }

    override fun getCameraSchedule(id: String): Mono<CameraSchedule> {
        return cameraCrud.findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("Camera with id $id not found")))
            .map {
                it.schedule ?: CameraSchedule(false, emptyList(), "00:00", "00:00")
            }
    }

    override fun getAllCameraSchedules(): Flux<Pair<String, CameraSchedule>> {
        return cameraCrud.findAll()
            .filter { it.schedule != null && it.schedule!!.enabled == true }
            .map { camera -> (camera.id to camera.schedule!!) as Pair<String, CameraSchedule>? }
    }


    override fun getCameraById(id: String): Mono<CameraBoundary> {
        return cameraCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("Camera with id $id not found")))
            .map { CameraBoundary(it) }
            .log()
    }

    override fun getCamerasPage(
        page: Int,
        size: Int
    ): Flux<CameraBoundary> {
        if (page < 0 || size < 1)
            return Flux.empty()

        return cameraCrud
            .findAllByIdNotNull(PageRequest.of(page, size, Sort.Direction.ASC, "name"))
            .map { CameraBoundary(it) }
            .log()
    }

    override fun getCamerasByEmail(
        email: String,
        page: Int,
        size: Int
    ): Flux<CameraBoundary> {
        if (page < 0 || size < 1)
            return Flux.empty()

        if (!isValidEmail(email))
            return Flux.error(BadRequestException400("Invalid email: $email"))

        return cameraCrud
            .findByEmailsContaining(email, PageRequest.of(page, size, Sort.Direction.ASC, "name"))
            .map { CameraBoundary(it) }
            .log()
    }

    override fun buildCamera(): Mono<Void> {
        return this.webClient
            .get()
            .uri("/build")
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { println("🚀 Camera build response: $it") }
            .onErrorResume { e ->
                Mono.error(BadRequestException400("Camera build failed: ${e.message}"))
            }
            .then()
            .log()
    }

    override fun deleteCamera(id: String): Mono<Void> {
        return cameraCrud
            .deleteById(id)
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return cameraCrud
            .deleteAll()
    }

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank())
            return false

        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return Regex(emailRegex).matches(email)
    }
}
