import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import java.util.UUID

private data class User(val id: Int, val email: String, val username: String, val password: String)

private data class RegisteringUser(val email: String, val registrationToken: String, val registrationCode: String, var emailVerified: Boolean = false)

private val registeringUsersTable = mutableListOf<RegisteringUser>()

private val usersTable = mutableListOf(
    User(1, "jon@snow.com", "Jon Snow", "AAbb1234!"),
    User(2, "amy@snow.com", "Amy Snow", "AAbb1234!"),
)

@Serializable
data class CheckEmailRequest(val email: String)

@Serializable
data class CheckEmailResponse(val timeout: Int, val registerToken: String, val message: String)

@Serializable
data class CheckEmailErrorResponse(val message: String)

fun Route.checkEmail() {
    post("/auth/signin/check_email") {
        val request = call.receive<CheckEmailRequest>()
        try {
            when {
                !isEmail(request.email) -> call.respond(HttpStatusCode.NotAcceptable, CheckEmailErrorResponse("Not an email"))
                usersTable.any { it.email == request.email } || registeringUsersTable.any { it.email == request.email } -> call.respond(HttpStatusCode.NotAcceptable, CheckEmailErrorResponse("Email already taken"))
                else -> {
                    // TODO: send real email with real code
                    val registrationToken = UUID.randomUUID().toString().encodeBase64()
                    registeringUsersTable += RegisteringUser(request.email, registrationToken, "00000")
                    call.respond(HttpStatusCode.OK, CheckEmailResponse(60000, registrationToken, "Input code from email (always 00000)"))
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, NotificationErrorResponse(e.message ?: "Unknown error"))
        }
    }
}

@Serializable
data class CheckCodeRequest(val code: String, val registrationToken: String)

@Serializable
data class CheckCodeResponse(val message: String)

fun Route.checkCode() {
    post("/auth/signin/check_code") {
        val request = call.receive<CheckCodeRequest>()
        val user = registeringUsersTable.firstOrNull { it.registrationToken == request.registrationToken }
        try {
            when {
                user == null -> call.respond(HttpStatusCode.NotAcceptable, CheckCodeResponse("Invalid token"))
                user.registrationCode != "00000" -> call.respond(HttpStatusCode.BadRequest, CheckCodeResponse("Invalid Code"))
                else -> {
                    registeringUsersTable.first { it.registrationToken == request.registrationToken }.emailVerified = true
                    call.respond(HttpStatusCode.OK, CheckCodeResponse("Enter password now"))
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, NotificationErrorResponse(e.message ?: "Unknown error"))
        }
    }
}

@Serializable
data class AddPasswordRequest(val password: String, val registrationToken: String)

@Serializable
data class AddPasswordResponse(val message: String)

fun Route.addPassword() {
    post("/auth/signin/password") {
        val request = call.receive<AddPasswordRequest>()
        try {
            val user = registeringUsersTable.firstOrNull { it.registrationToken == request.registrationToken }
            when {
                user == null -> call.respond(HttpStatusCode.NotAcceptable, AddPasswordResponse("Invalid token"))
                !user.emailVerified -> call.respond(HttpStatusCode.BadRequest, AddPasswordResponse("Email not verified"))
                else -> {
                    registeringUsersTable.remove(user)
                    usersTable += User(usersTable.last().id + 1, user.email, user.email, request.password)
                    call.respond(HttpStatusCode.OK, AddPasswordResponse("Success"))
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, NotificationErrorResponse(e.message ?: "Unknown error"))
        }
    }
}