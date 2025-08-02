package app.dataservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    val jwtUtil: JwtUtil
) {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers(
                    "/users/login",
                    "/users/create",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/webjars/**").permitAll()
                it.anyExchange().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(bearerServerAuthenticationEntryPoint()) }
            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun bearerServerAuthenticationEntryPoint(): ServerAuthenticationEntryPoint {
        return ServerAuthenticationEntryPoint { exchange, _ ->
            exchange.response.headers.add("Access-Control-Allow-Origin", "*")
            exchange.response.headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            exchange.response.headers.add("Access-Control-Allow-Headers", "*")
            exchange.response.headers.add("Access-Control-Max-Age", "3600")
            exchange.response.headers.add("WWW-Authenticate", "Bearer realm=\"Realm\"")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            Mono.empty()
        }
    }

    @Bean
    fun jwtAuthenticationFilter(): AuthenticationWebFilter {
        val authManager = ReactiveAuthenticationManager { auth ->
            val token = auth.credentials.toString()
            if (jwtUtil.validateToken(token)) {
                val userId = jwtUtil.getUserIdFromToken(token)
                val roles = jwtUtil.getRolesFromToken(token)

                // Assuming roles are in the format "ROLE_USER", "ROLE_ADMIN", etc.
                val authorities = roles.map { role -> SimpleGrantedAuthority("ROLE_$role") }
                Mono.just(UsernamePasswordAuthenticationToken(userId, token, authorities))
            } else {
                Mono.empty()
            }
        }

        val filter = AuthenticationWebFilter(authManager)
        filter.setServerAuthenticationConverter { exchange ->
            val authHeader = exchange.request.headers.getFirst("Authorization")
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.removePrefix("Bearer ")
                Mono.just(UsernamePasswordAuthenticationToken(null, token))
            } else {
                print("No Authorization header found or invalid format")
                Mono.empty()
            }
        }
        return filter
    }
}

