package app.dataservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsGlobalConfig {

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("*") // אפשר להחליף בכתובת ספציפית כמו "http://localhost:3000"
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsWebFilter(source)
    }
}
