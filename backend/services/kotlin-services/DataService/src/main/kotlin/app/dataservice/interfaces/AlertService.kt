package app.dataservice.interfaces

import app.dataservice.boundaries.AlertBoundary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AlertService {
    fun createAlert(alert: AlertBoundary): Mono<AlertBoundary>
    fun getAlertById(id: String): Mono<AlertBoundary>
    fun getAlertsPage(page: Int, size: Int): Flux<AlertBoundary>
    fun getAlertsByVehicle(id: String, page: Int, size: Int): Flux<AlertBoundary>
    fun getAlertsByTimestampAfter(timestampStr: String, page: Int, size: Int): Flux<AlertBoundary>
    fun getAlertsByCameraId(cameraId: String, page: Int, size: Int): Flux<AlertBoundary>
    fun deleteAlert(id: String): Mono<Void>
    fun deleteAll(): Mono<Void>
}