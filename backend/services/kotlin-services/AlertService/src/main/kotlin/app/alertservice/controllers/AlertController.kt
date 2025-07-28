package app.alertservice.controllers

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.interfaces.AlertService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasRole('ADMIN')")
    fun createAlert(
        @RequestBody alert: AlertBoundary,
        request: ServerHttpRequest
    ): Mono<AlertBoundary> {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        println("Authorization Header: $authHeader")

        return this.alertService
            .sendAlert(alert)
            .then(
                this.alertService
                    .createAlert(alert, authHeader!!)
                    .onErrorResume { error -> Mono.error(error) }
            )
    }
}
