package app.alertservice.controllers

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.interfaces.AlertService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@SecurityRequirement(name = "BearerAuth")
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
            .sendAlert(alert)
            .then(
                this.alertService
                    .createAlert(alert)
            )
    }
}
