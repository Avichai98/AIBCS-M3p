package app.dataservice.interfaces

import app.dataservice.entities.CameraEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface CameraCrud : ReactiveMongoRepository<CameraEntity, String> {
    fun findAllByIdNotNull(pageable: Pageable): Flux<CameraEntity>
    fun findByEmailsContaining(email: String, pageable: Pageable): Flux<CameraEntity>
}