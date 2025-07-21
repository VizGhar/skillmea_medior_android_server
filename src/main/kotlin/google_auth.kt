import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.net.URL

private fun verifyGoogleToken(idToken: String): JWTClaimsSet? {
    val jwkSource = JWKSourceBuilder.create<SecurityContext>(URL("https://www.googleapis.com/oauth2/v3/certs")).build()
    val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
    jwtProcessor.jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)

    return try {
        val context: SecurityContext? = null
        val claims = jwtProcessor.process(idToken, context)
        if (claims.issuer != "https://accounts.google.com" && claims.issuer != "accounts.google.com") return null
        if (!claims.audience.contains("???.apps.googleusercontent.com")) return null
        claims
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Serializable
data class GoogleLoginVerifyRequest(val idToken: String)

@Serializable
data class GoogleLoginVerifyErrorResponse(val message: String)

@Serializable
data class GoogleLoginVerifySuccessResponse(val email: String, val authToken: String)

fun Route.googleLoginVerify() {
    post("/verify") {
        try {
            val body = call.receive<GoogleLoginVerifyRequest>()
            val token = body.idToken
            val claims = verifyGoogleToken(token)
            if (claims == null) {
                call.respond(HttpStatusCode.Unauthorized, GoogleLoginVerifyErrorResponse("Invalid token"))
            } else {
                val email = claims.getStringClaim("email")
                call.respond(GoogleLoginVerifySuccessResponse(email, email))
            }
        } catch (ex: Exception) {
            call.respond(HttpStatusCode.InternalServerError, GoogleLoginVerifyErrorResponse("Invalid request"))
        }
    }
}
