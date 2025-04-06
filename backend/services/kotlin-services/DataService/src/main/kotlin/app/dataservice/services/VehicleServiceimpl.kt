package app.dataservice.services

import app.dataservice.boundaries.VehicleBoundary
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.VehicleCrud
import app.dataservice.interfaces.VehicleService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

@Service
class VehicleServiceimpl(
    val vehicleCrud: VehicleCrud
) : VehicleService {
    override fun createVehicle(vehicle: VehicleBoundary): Mono<VehicleBoundary> {
        return Mono.just(vehicle)
            .flatMap {
                if(vehicle.type.isNullOrBlank() || vehicle.imageUrl.isNullOrBlank()
                    || vehicle.description.isNullOrBlank() || vehicle.color.isNullOrBlank() || vehicle.latitude == null
                    || vehicle.longitude == null)
                    Mono.error (BadRequestException400("Required fields are missing"))
                else{
                    vehicle.id = null
                    vehicle.timestamp = Date()
                    vehicle.stayDuration = 0

                    Mono.just(vehicle)
                }
            }
            .map { vehicle.toEntity() }
            .flatMap { this.vehicleCrud.save(it) }
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun updateVehicle(
        id: String,
        updatedVehicle: VehicleBoundary
    ): Mono<VehicleBoundary> {
        return vehicleCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("User with the id: $id not found")))
            .flatMap {
                if(!updatedVehicle.imageUrl.isNullOrBlank())
                    it.imageUrl = updatedVehicle.imageUrl

                if(updatedVehicle.latitude != null)
                    it.latitude = updatedVehicle.latitude

                if(updatedVehicle.longitude != null)
                    it.longitude = updatedVehicle.longitude

                it.stayDuration = Date().time - it.timestamp!!.time
                this.vehicleCrud.save(it)
            }
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun deleteVehicle(
        id: String,
        vehicle: VehicleBoundary
    ): Mono<Void> {
        return this.vehicleCrud
            .deleteById(id)
            .log()
    }

    override fun getVehiclesPage(
        page: Int,
        size: Int
    ): Flux<VehicleBoundary> {
        if(page < 0 || size < 1)
            return Flux.empty()

        return vehicleCrud
            .findAllByIdNotNull(PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun getVehicleById(id: String): Mono<VehicleBoundary> {
        return vehicleCrud
            .findById(id)
            .switchIfEmpty (Mono.error(NotFoundException404("Vehicle with id $id not found")))
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun getVehiclesByManufacturer(
        manufacturer: String,
        page: Int,
        size: Int
    ): Flux<VehicleBoundary> {
        return this.vehicleCrud
            .findAllByManufacturer(manufacturer, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun getVehiclesByLatitudeAndLongitude(
        latitude: Double,
        longitude: Double,
        page: Int,
        size: Int
    ): Flux<VehicleBoundary> {
        return this.vehicleCrud
            .findByLatitudeAndLongitude(latitude, longitude, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun getVehiclesAfterTimestamp(
        date: Date,
        page: Int,
        size: Int
    ): Flux<VehicleBoundary> {
        return this.vehicleCrud
            .findAllByTimestampAfter(date, PageRequest.of(page, size, Sort.Direction.ASC, "timestamp"))
            .map { VehicleBoundary(it) }
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return vehicleCrud
            .deleteAll()
    }
}