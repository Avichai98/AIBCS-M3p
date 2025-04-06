package app.dataservice.interfaces

import app.dataservice.boundaries.UserBoundary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserService {
    fun createUser(user: UserBoundary): Mono<UserBoundary>
    fun updateUser(id: String, user: UserBoundary): Mono<Void>
    fun deleteUser(id: String): Mono<Void>
    fun getUserById(id: String): Mono<UserBoundary>
    fun getUserByEmail(email: String): Mono<UserBoundary>
    fun getUsersPage(page: Int, size: Int): Flux<UserBoundary>
    fun deleteAll(): Mono<Void>
}