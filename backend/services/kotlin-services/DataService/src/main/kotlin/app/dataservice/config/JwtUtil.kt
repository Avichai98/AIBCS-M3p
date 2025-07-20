package app.dataservice.config

import app.dataservice.entities.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtil {
    private val jwtSecret = System.getenv("JWT_SECRET")
        ?: throw IllegalStateException("Missing JWT_SECRET environment variable")

    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateToken(user: UserEntity): String {
        val now = Date()
        val expiry = Date(now.time + 3600000)
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
        Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).body
}
