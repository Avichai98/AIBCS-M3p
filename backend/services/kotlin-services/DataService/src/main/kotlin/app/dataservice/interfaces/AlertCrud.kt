package app.dataservice.interfaces

import app.dataservice.entities.AlertEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface AlertCrud : ReactiveMongoRepository<AlertEntity, String> {
    fun findAllByIdNotNull(pageable: Pageable): Flux<AlertEntity>
    fun findAllByVehicleEntityId(id: String, pageable: Pageable): Flux<AlertEntity>
    fun findAllByTimestampAfter(timestamp: LocalDateTime, pageable: Pageable): Flux<AlertEntity>
    fun findAllByCameraId(cameraId: String, pageable: Pageable): Flux<AlertEntity>
}