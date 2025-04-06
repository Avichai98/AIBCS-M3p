package app.dataservice.services

import app.dataservice.boundaries.UserBoundary
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.UserCrud
import app.dataservice.interfaces.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Date

@Service
class UserServiceimpl(
    val userCrud: UserCrud
):
    UserService {
    override fun createUser(user: UserBoundary): Mono<UserBoundary> {
        return Mono.just(user)
            .flatMap {
                if (user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank() || user.mobile.isNullOrBlank()
                    || user.username.isNullOrBlank())
                    Mono.error (BadRequestException400("Required fields are missing"))
                else if (!isValidEmail(user.email))
                    Mono.error(BadRequestException400("Invalid email"))
                else {
                    user.id = null
                    user.createdAt = Date()

                    Mono.just(it)
                }
            }
            .map { user.toEntity() }
            .flatMap { this.userCrud.save(it) }
            .map { UserBoundary(it) }
            .log()
    }

    override fun updateUser(
        id: String,
        user: UserBoundary
    ): Mono<Void> {
        return userCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("User with the id: $id not found")))
            .flatMap {
                if (!user.firstName.isNullOrBlank())
                    it.firstName = user.firstName
                if (!user.lastName.isNullOrBlank())
                    it.lastName = user.lastName
                if (!user.username.isNullOrBlank())
                    it.username = user.username

                it.updatedAt = Date()
                this.userCrud.save(it)
            }
            .log()
            .then()
    }

    override fun deleteUser(
        id: String,
        user: UserBoundary
    ): Mono<Void> {
        return userCrud
            .deleteById(id)
            .log()
    }

    override fun getUserById(id: String): Mono<UserBoundary> {
        return userCrud
            .findById(id)
            .switchIfEmpty (Mono.error(NotFoundException404("User with id $id not found")))
            .map { UserBoundary(it) }
            .log()
    }

    override fun getUserByEmail(email: String): Mono<UserBoundary> {
        return userCrud
            .findByEmail(email)
            .switchIfEmpty(Mono.error(NotFoundException404("User with the email: $email not found")))
            .map { UserBoundary(it) }
            .log()
    }

    override fun getUsersPage(
        page: Int,
        size: Int
    ): Flux<UserBoundary> {
        if(page < 0 || size < 1)
            return Flux.empty()
        
        return userCrud
            .findAllByIdNotNull(PageRequest.of(page, size, Sort.Direction.ASC, "username"))
            .map { UserBoundary(it) }
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return userCrud.deleteAll()
    }

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank())
            return false

        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return Regex(emailRegex).matches(email)
    }
}