import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main() {
    initNotifications()

    embeddedServer(Netty, port = 3000) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                disableHtmlEscaping()
            }
        }
        routing {
            notifyRoute()
            checkEmail()
            checkCode()
            addPassword()
        }
    }.start(wait = true)
}