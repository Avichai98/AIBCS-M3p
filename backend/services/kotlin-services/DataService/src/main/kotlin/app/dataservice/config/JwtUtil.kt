package app.dataservice.config

import app.dataservice.entities.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtil(
    @Value("\${JWT_SECRET}")
    jwtSecret: String
) {
    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateToken(user: UserEntity): String {
        val now = Date()
        val expiry = Date(now.time + 3600000) // 1 hour
        return Jwts.builder()
            .setSubject(user.id)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("email", user.email)
            .signWith(key)
            .compact()
    }

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
