import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.FileInputStream

fun initNotifications() {
    val serviceAccount = FileInputStream("skillmea.json")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    FirebaseApp.initializeApp(options)
}

@Serializable
data class NotificationRequest(val token: String, val title: String, val body: String)

@Serializable
data class NotificationResponse(val messageId: String)

@Serializable
data class NotificationErrorResponse(val message: String)

fun Route.notifyRoute() {
    post("/notify") {
        val request = call.receive<NotificationRequest>()

        val message = Message.builder()
            .setToken(request.token)
            .setNotification(Notification.builder().setTitle(request.title).setBody(request.body).build())
            .build()

        try {
            val response = FirebaseMessaging.getInstance().send(message)
            call.respond(HttpStatusCode.OK, NotificationResponse(response))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, NotificationErrorResponse(e.message ?: "Unknown error"))
        }
    }
}

