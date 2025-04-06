package app.dataservice.contollers

import app.dataservice.boundaries.AlertBoundary
import app.dataservice.interfaces.AlertService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/alert")
class AlertController(
    val alertService: AlertService
) {
    @PostMapping(
        path = ["/createAlert"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createAlert(
        @RequestBody alert: AlertBoundary
    ): Mono<AlertBoundary>{
        return this.alertService
        .createAlert(alert)
    }

    @DeleteMapping(
        path = ["/deleteAlert/{id}"]
    )
    fun deleteAlert(
        @PathVariable id: String
    ): Mono<Void>{
        return this.alertService
        .deleteAlert(id)
    }

    @GetMapping(
        path = ["/getAlertById/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAlertById(
        @PathVariable id: String
    ): Mono<AlertBoundary>{
        return this.alertService
        .getAlertById(id)
    }

    @GetMapping(
        path = ["/getAlerts"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getAlertsPage(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<AlertBoundary>{
        return this.alertService
        .getAlertsPage(page, size)
    }

    @GetMapping(
        path = ["/getAlertsByVehicle/{id}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getAlertsByVehicle(
        @PathVariable id: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<AlertBoundary>{
        return this.alertService
        .getAlertsByVehicle(id, page, size)
    }

    @GetMapping(
        path = ["/getAlertsByTimestamp"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAlertsByTimestamp(
        @RequestParam("timestampStr") timestampStr: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<AlertBoundary>{
        return this.alertService
            .getAlertsByTimestampAfter(timestampStr, page, size)
    }

    @DeleteMapping(
        path = ["/deleteAllAlerts"]
    )
    fun deleteAllAlerts(): Mono<Void>{
        return this.alertService
        .deleteAll()
    }
}