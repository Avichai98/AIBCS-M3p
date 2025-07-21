package app.alertservice.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtil(
    @Value("\${JWT_SECRET}")
    jwtSecret: String
) {
    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun validateToken(token: String): Boolean = try {
        getClaims(token)
        true
    } catch (e: Exception) {
        false
    }

    fun getUserIdFromToken(token: String): String =
        getClaims(token).subject

    fun getClaims(token: String) =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}
