package app.dataservice.interfaces

import app.dataservice.boundaries.VehicleBoundary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

interface VehicleService {
    fun createVehicle(vehicle: VehicleBoundary): Mono<VehicleBoundary>
    fun updateVehicle(id: String, updatedVehicle: VehicleBoundary): Mono<VehicleBoundary>
    fun deleteVehicle(id: String, vehicle: VehicleBoundary): Mono<Void>
    fun getVehiclesPage(page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehicleById(id: String): Mono<VehicleBoundary>
    fun getVehiclesByManufacturer(manufacturer: String, page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehiclesByLatitudeAndLongitude(latitude: Double, longitude: Double, page: Int, size: Int): Flux<VehicleBoundary>
    fun getVehiclesAfterTimestamp(date: Date, page: Int, size: Int): Flux<VehicleBoundary>
    fun deleteAll(): Mono<Void>
}