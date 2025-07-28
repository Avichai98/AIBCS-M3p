package app.dataservice.services

import app.dataservice.boundaries.AlertBoundary
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.AlertCrud
import app.dataservice.interfaces.AlertService
import app.dataservice.interfaces.CameraCrud
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AlertServiceImpl(
    val alertCrud: AlertCrud,
    val cameraCrud: CameraCrud
):
    AlertService {
    override fun createAlert(alert: AlertBoundary): Mono<AlertBoundary> {
        return Mono.just(alert)
            .flatMap {
                if(alert.type.isNullOrBlank() || alert.severity.isNullOrBlank() || alert.description.isNullOrBlank())
                    Mono.error(BadRequestException400("Required fields are missing"))
                else if (alert.vehicleBoundary == null)
                    Mono.error(BadRequestException400("Vehicle is missing!"))
                else{
                    alert.id = null
                    alert.timestamp = LocalDateTime.now()

                    Mono.just(it)
                }
            }
            .flatMap {
                cameraCrud.findById(alert.cameraId!!)
                    .switchIfEmpty(Mono.error(BadRequestException400("Camera with id ${alert.cameraId} not found")))
                    .flatMap  {
                        it.alertCount = it.alertCount!! + 1

                        cameraCrud.save(it)
                    }
                    .log()
            }
            .map { alert.toEntity() }
            .flatMap { this.alertCrud.save(it) }
            .map { AlertBoundary(it) }
            .log()
    }

    override fun getAlertById(id: String): Mono<AlertBoundary> {
        return this.alertCrud
            .findById(id)
            .switchIfEmpty (Mono.error(NotFoundException404("Alert with id $id not found")))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun getAlertsPage(
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        if(page < 0 || size < 1)
            return Flux.empty()

        return this.alertCrud
            .findAllByIdNotNull(PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun getAlertsByVehicle(
        id: String,
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        if(page < 0 || size < 1)
            return Flux.empty()

        return this.alertCrud
            .findAllByVehicleEntityId(id, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun getAlertsByTimestampAfter(
        timestampStr: String,
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        if (page < 0 || size < 1)
            return Flux.empty()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return Mono.fromCallable { LocalDateTime.parse(timestampStr, formatter) }
            .onErrorResume {
                Mono.error(BadRequestException400("Invalid date format: Use YYYY-MM-DD'T'HH:mm:ss"))
            }
            .flatMapMany { timestamp ->
                this.alertCrud
                    .findAllByTimestampAfter(timestamp, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
                    .map {
                        AlertBoundary(it)
                    }
                    .log()
            }
    }

    override fun getAlertsByCameraId(
        cameraId: String,
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        if (page < 0 || size < 1)
            return Flux.empty()

        return this.alertCrud
            .findAllByCameraId(cameraId, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun deleteAlert(
        id: String,
    ): Mono<Void> {
        return this.alertCrud
            .deleteById(id)
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return this.alertCrud
            .deleteAll()
    }
}