package app.dataservice.config

import org.springframework.security.core.Authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthFilter(
    private val jwtUtil: JwtUtil
) : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val authHeader = exchange.request.headers.getFirst("Authorization") ?: return Mono.empty()
        if (!authHeader.startsWith("Bearer ")) return Mono.empty()

        val token = authHeader.removePrefix("Bearer ")

        val claims = try {
            jwtUtil.getClaims(token)
        } catch (e: Exception) {
            return Mono.empty()
        }

        val email = claims["email"] as? String ?: return Mono.empty()
        val auth = UsernamePasswordAuthenticationToken(email, null, emptyList())

        return Mono.just(auth)
    }
}
