package app.dataservice.interfaces

import app.dataservice.boundaries.AlertBoundary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

interface AlertService {
    fun createAlert(alert: AlertBoundary): Mono<AlertBoundary>
    fun deleteAlert(id: String): Mono<Void>
    fun getAlertById(id: String): Mono<AlertBoundary>
    fun getAlertsPage(page: Int, size: Int): Flux<AlertBoundary>
    fun getAlertsByVehicle(id: String, page: Int, size: Int): Flux<AlertBoundary>
    fun getAlertsByTimestampAfter(timestamp: String, page: Int, size: Int): Flux<AlertBoundary>
    fun deleteAll(): Mono<Void>
}