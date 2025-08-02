package app.alertservice.interfaces

import app.alertservice.boundaries.AlertBoundary
import reactor.core.publisher.Mono

interface AlertService {
    fun createAlert(alert: AlertBoundary, authorizationHeader: String): Mono<AlertBoundary>
    fun sendAlert(alert: AlertBoundary): Mono<Void>
}