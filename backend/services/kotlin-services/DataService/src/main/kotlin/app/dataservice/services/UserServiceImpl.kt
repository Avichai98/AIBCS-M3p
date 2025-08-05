package app.dataservice.services

import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import app.dataservice.boundaries.UserBoundary
import app.dataservice.config.JwtUtil
import app.dataservice.exceptions.BadRequestException400
import app.dataservice.exceptions.NotFoundException404
import app.dataservice.interfaces.UserCrud
import app.dataservice.interfaces.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class UserServiceImpl(
    val userCrud: UserCrud,
    val jwtUtil: JwtUtil,
    val passwordEncoder: PasswordEncoder
) :
    UserService {
    override fun createUser(user: UserBoundary): Mono<UserBoundary> {
        return Mono.just(user)
            .flatMap {
                if (user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank() ||
                    user.mobile.isNullOrBlank() || user.username.isNullOrBlank()
                ) {
                    Mono.error(BadRequestException400("Required fields are missing"))
                } else if (!isValidEmail(user.email)) {
                    Mono.error(BadRequestException400("Invalid email"))
                } else {
                    userCrud.findByEmail(user.email!!)
                        .flatMap<UserBoundary> {
                            Mono.error(BadRequestException400("User with this email already exists"))
                        }
                        .switchIfEmpty(
                            Mono.defer {
                                user.id = null
                                user.createdAt = LocalDateTime.now()
                                user.password = passwordEncoder.encode(user.password)
                                userCrud.save(user.toEntity())
                                    .map { UserBoundary(it) }
                            }
                        )
                }
            }
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
                if (!user.mobile.isNullOrBlank())
                    it.mobile = user.mobile
                if (!user.password.isNullOrBlank())
                    it.password = passwordEncoder.encode(user.password)
                if (user.roles.isNotEmpty())
                    it.roles = user.roles

                it.updatedAt = LocalDateTime.now()
                this.userCrud.save(it)
            }
            .log()
            .then()
    }

    override fun getUserById(id: String): Mono<UserBoundary> {
        return userCrud
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException404("User with id $id not found")))
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
        if (page < 0 || size < 1)
            return Flux.empty()

        return userCrud
            .findAllByIdNotNull(PageRequest.of(page, size, Sort.Direction.ASC, "username"))
            .map { UserBoundary(it) }
            .log()
    }

    override fun deleteUser(
        id: String,
    ): Mono<Void> {
        return userCrud
            .deleteById(id)
            .log()
    }

    override fun deleteAll(): Mono<Void> {
        return userCrud.deleteAll()
    }

    override fun login(login: LoginBoundary): Mono<LoginResponse> {
        System.err.println("***** UserServiceImpl.login: User found with: email: ${login.email}, password: ${login.password}")
        return userCrud
            .findByEmail(login.email)
            .switchIfEmpty(Mono.error(BadRequestException400("Invalid email or password")))
            .flatMap { user ->
                if (!passwordEncoder.matches(login.password, user.password)) {
                    System.err.println("***** UserServiceImpl.login: User found with email inside if")
                    Mono.error(BadRequestException400("Invalid email or password"))
                } else {
                    System.err.println("***** UserServiceImpl.login: User found with email: ${user.email}")
                    val token = jwtUtil.generateToken(user)
                    val response = LoginResponse(UserBoundary(user), token)
                    Mono.just(response)
                }
            }
    }

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank())
            return false

        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return Regex(emailRegex).matches(email)
    }
}