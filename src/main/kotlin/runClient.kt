import com.sksamuel.hoplite.ConfigLoader
import java.io.File

data class Credentials(
    val username: String,
    val password: String
)

fun main() {
    val host = "smtp-relay.sendinblue.com"
    val port = 587

    val creds = ConfigLoader().loadConfigOrThrow<Credentials>(File("creds.yaml"))
    val client = MailClient(host, port, creds, "me@ilya.voronin.ru")

    println("Enter email:\n")
    val emailTo = readLine()!!

    client.send(emailTo, "Test", "Hi,\n this is a test message.", listOf(File("doge.jpeg")))
}