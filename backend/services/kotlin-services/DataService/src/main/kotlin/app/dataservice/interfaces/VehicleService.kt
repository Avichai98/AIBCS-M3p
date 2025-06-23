package app.dataservice.interfaces

import app.dataservice.boundaries.VehicleBoundary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface VehicleService {
    fun createVehicle(vehicle: VehicleBoundary): Mono<VehicleBoundary>
    fun updateVehicle(id: String, updatedVehicle: VehicleBoundary): Mono<VehicleBoundary>
    fun getVehicleById(id: String): Mono<VehicleBoundary>
    fun getVehiclesPage(page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehiclesByManufacturer(manufacturer: String, page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehiclesByLatitudeAndLongitude(latitude: Double, longitude: Double, page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehiclesByTimestampAfter(timestampStr: String, page: Int, size: Int): Flux<VehicleBoundary>
    fun deleteVehicle(id: String): Mono<Void>
    fun deleteAll(): Mono<Void>
}