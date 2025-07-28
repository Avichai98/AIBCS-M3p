package app.dataservice.interfaces

import app.dataservice.entities.VehicleEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface VehicleCrud : ReactiveMongoRepository<VehicleEntity, String> {
    fun findAllByIdNotNull(pageable: Pageable): Flux<VehicleEntity>
    fun findAllByManufacturer(manufacturer: String, pageable: Pageable): Flux<VehicleEntity>
    fun findByLatitudeAndLongitude(latitude: Double, longitude: Double, page: Pageable): Flux<VehicleEntity>
    fun findAllByTimestampAfter(date: LocalDateTime, page: Pageable): Flux<VehicleEntity>
}