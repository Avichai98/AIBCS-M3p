package app.dataservice.controllers

import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.CameraSchedule
import app.dataservice.interfaces.CameraService
import app.dataservice.scheduling.CameraScheduler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
    fun create(
        @RequestBody camera: CameraBoundary
    ): Mono<CameraBoundary> {
        return this.cameraService
            .createCamera(camera)
    }

    @PostMapping(
        path = ["/start"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun startCamera(): Mono<Void> {
        return this.cameraService
            .startCamera()
    }

    @PostMapping(
        path = ["/stop"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun stopCamera(): Mono<Void> {
        return this.cameraService
            .stopCamera()
    }

    @PutMapping(
        path = ["/update/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun update(
        @PathVariable id: String,
        @RequestBody camera: CameraBoundary
    ): Mono<Void> {
        return this.cameraService
            .updateCamera(id, camera)
    }

    @PutMapping("/schedule/{id}")
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

    @GetMapping("/schedule/{id}")
    fun getCameraSchedule(@PathVariable id: String): Mono<CameraSchedule> {
        return cameraService
            .getCameraSchedule(id)
    }

    @GetMapping(
        path = ["/getCameraById/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
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
    fun buildCamera(): Mono<Void> {
        return this.cameraService
            .buildCamera()
    }

    @DeleteMapping(
        path = ["/delete/{id}"],
    )
    fun delete(
        @PathVariable id: String
    ): Mono<Void> {
        return this.cameraService
            .deleteCamera(id)
    }

    @DeleteMapping(
        path = ["/deleteAllCameras"]
    )
    fun deleteAllCameras(
    ): Mono<Void> {
        return this.cameraService
            .deleteAll()
    }
}