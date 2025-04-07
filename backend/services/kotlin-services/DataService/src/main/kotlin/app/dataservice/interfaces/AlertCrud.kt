package app.dataservice.interfaces

import app.dataservice.entities.AlertEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.util.Date

interface AlertCrud : ReactiveMongoRepository<AlertEntity, String> {
    fun findAllByIdNotNull(pageable: Pageable): Flux<AlertEntity>
    fun findAllByVehicleEntityId(id: String, pageable: Pageable): Flux<AlertEntity>
    fun findAllByTimestampAfter(timestamp: Date, page: Pageable): Flux<AlertEntity>
}