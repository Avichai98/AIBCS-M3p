package app.dataservice.controllers

import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.CameraSchedule
import app.dataservice.interfaces.CameraService
import app.dataservice.scheduling.CameraScheduler
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/cameras")
class CameraController(
    val cameraService: CameraService,
    val cameraScheduler: CameraScheduler
) {
    @PostMapping(
        path = ["/create"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun create(
        @RequestBody camera: CameraBoundary
    ): Mono<CameraBoundary> {
        return this.cameraService
            .createCamera(camera)
    }

    @GetMapping(
        path = ["/start"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun startCamera(request: ServerHttpRequest): Mono<Void> {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        return this.cameraService
            .startCamera(authHeader!!)
            .onErrorResume { error -> Mono.error(error) }
    }

    @GetMapping(
        path = ["/stop"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun stopCamera(request: ServerHttpRequest): Mono<Void> {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return this.cameraService
            .stopCamera(authHeader!!)
            .onErrorResume { error -> Mono.error(error) }
    }

    @PutMapping(
        path = ["/update/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @PathVariable id: String,
        @RequestBody camera: CameraBoundary
    ): Mono<Void> {
        return this.cameraService
            .updateCamera(id, camera)
    }

    @PutMapping("/schedule/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun updateCameraSchedule(
        @PathVariable id: String,
        @RequestBody schedule: CameraSchedule
    ): Mono<Void> {
        return cameraService
            .updateCameraSchedule(id, schedule)
            .doOnSuccess {
                // Once saved, also trigger rescheduling
                cameraScheduler.scheduleCamera(id, schedule)
            }
    }

    @PutMapping(
        path = ["/status/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun updateCameraStatus(
        @PathVariable id: String,
        @RequestBody status: Boolean
    ): Mono<Void> {
        return this.cameraService
            .updateCameraStatus(id, status)
    }

    @GetMapping("/schedule/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun getCameraSchedule(@PathVariable id: String): Mono<CameraSchedule> {
        return cameraService
            .getCameraSchedule(id)
    }

    @GetMapping(
        path = ["/getCameraById/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun getCameraById(
        @PathVariable id: String
    ): Mono<CameraBoundary> {
        return this.cameraService
            .getCameraById(id)
    }

    @GetMapping(
        path = ["/getCameras"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllCameras(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<CameraBoundary> {
        return this.cameraService
            .getCamerasPage(page, size)
    }

    @GetMapping(
        path = ["/getCamerasByEmail/{email}"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun getCamerasByEmail(
        @PathVariable email: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ):
            Flux<CameraBoundary> {
        return this.cameraService
            .getCamerasByEmail(email, page, size)
    }

    @GetMapping(
        path = ["/build"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun buildCamera(request: ServerHttpRequest): Mono<Void> {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        return this.cameraService
            .buildCamera(authHeader!!)
            .onErrorResume { error -> Mono.error(error) }
    }

    @DeleteMapping(
        path = ["/delete/{id}"],
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(
        @PathVariable id: String
    ): Mono<Void> {
        return this.cameraService
            .deleteCamera(id)
    }

    @DeleteMapping(
        path = ["/deleteAllCameras"]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteAllCameras(
    ): Mono<Void> {
        return this.cameraService
            .deleteAll()
    }
}