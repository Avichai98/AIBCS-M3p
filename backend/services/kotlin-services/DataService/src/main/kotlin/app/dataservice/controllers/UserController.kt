package app.dataservice.controllers

import app.dataservice.boundaries.LoginBoundary
import app.dataservice.boundaries.LoginResponse
import app.dataservice.boundaries.UserBoundary
import app.dataservice.interfaces.UserService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/users")
class UserController(
    val userService: UserService
) {
    @PostMapping(
        path = ["/create"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun create(
        @RequestBody user: UserBoundary
    ): Mono<UserBoundary> {
        return this.userService
            .createUser(user)
    }

    @PutMapping(
        path = ["/update/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    fun update(
        @PathVariable id: String,
        @RequestBody user: UserBoundary
    ): Mono<Void> {
        return this.userService
            .updateUser(id, user)
    }

    @GetMapping(
        path = ["/getUserById/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserById(
        @PathVariable id: String
    ): Mono<UserBoundary> {
        return this.userService
            .getUserById(id)
    }

    @GetMapping(
        path = ["/getUserByEmail/{email}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByEmail(
        @PathVariable email: String
    ): Mono<UserBoundary> {
        return this.userService
            .getUserByEmail(email)
    }

    @GetMapping(
        path = ["/getUsers"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int
    ): Flux<UserBoundary> {
        return this.userService
            .getUsersPage(page, size)
    }

    @DeleteMapping(
        path = ["/delete/{id}"],
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(
        @PathVariable id: String
    ): Mono<Void> {
        return this.userService
            .deleteUser(id)
    }

    @DeleteMapping(
        path = ["/deleteAllUsers"]
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteAllUsers(
    ): Mono<Void> {
        return this.userService
            .deleteAll()
    }

    @PostMapping(
        path = ["/login"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun login(
        @RequestBody login: LoginBoundary
    ): Mono<LoginResponse> {
        return this.userService
            .login(login)
    }
}