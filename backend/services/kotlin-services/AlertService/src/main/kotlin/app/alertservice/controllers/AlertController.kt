package app.alertservice.controllers

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.interfaces.AlertService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/alerts")
class AlertController(
    private val alertService: AlertService
) {

    @PostMapping(
        path = ["/create"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createAlert(@RequestBody alert: AlertBoundary): Mono<AlertBoundary> {
        return this.alertService
            .createAlert(alert)
            .flatMap { alertService.sendAlert(it).thenReturn(it) }
    }
}
