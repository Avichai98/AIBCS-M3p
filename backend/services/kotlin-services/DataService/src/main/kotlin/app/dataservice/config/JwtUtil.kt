package app.dataservice.config

import app.dataservice.entities.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtil {
    // Secret key for signing JWT tokens - keep this safe and don't expose publicly
    private val jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS256)

    // Generate JWT token based on user information
    fun generateToken(user: UserEntity): String {
        val now = Date()
        val expiryDate = Date(now.time + 3600000) // Token valid for 1 hour

        return Jwts.builder()
            .setSubject(user.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("email", user.email)
            .signWith(jwtSecret)
            .compact()
    }
}