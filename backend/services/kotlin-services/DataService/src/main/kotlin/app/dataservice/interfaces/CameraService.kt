package app.dataservice.interfaces

import app.dataservice.boundaries.CameraBoundary
import app.dataservice.boundaries.CameraSchedule
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CameraService {
    fun createCamera(camera: CameraBoundary): Mono<CameraBoundary>
    fun updateCamera(id: String, camera: CameraBoundary): Mono<Void>
    fun updateCameraSchedule(id: String, schedule: CameraSchedule): Mono<Void>
    fun getCameraSchedule(id: String): Mono<CameraSchedule>
    fun getAllCameraSchedules(): Flux<Pair<String, CameraSchedule>>
    fun getCameraById(id: String): Mono<CameraBoundary>
    fun getCamerasPage(page: Int, size: Int): Flux<CameraBoundary>
    fun getCamerasByEmail(email: String, page: Int, size: Int): Flux<CameraBoundary>
    fun deleteCamera(id: String): Mono<Void>
    fun deleteAll(): Mono<Void>
}