package app.dataservice.services

import app.dataservice.boundaries.AlertBoundary
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.AlertCrud
import app.dataservice.interfaces.AlertService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

@Service
class AlertServiceimpl(
    val alertCrud: AlertCrud
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
                    alert.timestamp = Date()

                    Mono.just(it)
                }
            }
            .map { alert.toEntity() }
            .flatMap { this.alertCrud.save(it) }
            .map { AlertBoundary(it) }
            .log()
    }

    override fun deleteAlert(
        id: String,
        alert: AlertBoundary
    ): Mono<Void> {
        return this.alertCrud
            .deleteById(id)
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

    override fun getAllByVehicle(
        id: String,
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        return this.alertCrud
            .findAllByVehicleEntityId(id, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun getAllByTimestampAfter(
        date: Date,
        page: Int,
        size: Int
    ): Flux<AlertBoundary> {
        return this.alertCrud
            .findAllByTimestampAfter(date, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { AlertBoundary(it) }
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return this.alertCrud
            .deleteAll()
    }

}