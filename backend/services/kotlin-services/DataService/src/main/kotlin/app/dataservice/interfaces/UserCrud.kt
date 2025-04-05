package app.dataservice.interfaces

import app.dataservice.entities.UserEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserCrud : ReactiveMongoRepository<UserEntity, String> {
    fun findAllByIdNotNull(pageable: Pageable): Flux<UserEntity>
    fun findByEmail(email: String): Mono<UserEntity>
}