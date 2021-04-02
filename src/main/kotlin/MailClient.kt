import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.util.*


class MailClient(host: String, port: Int, private val creds: Credentials, private val myEmail: String) {
    private val client = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    private var myInput: ByteReadChannel
    private var myOutput: ByteWriteChannel

    init {
        runBlocking {
            val socket = client.connect(InetSocketAddress(host, port))
            myOutput = socket.openWriteChannel(autoFlush = true)
            myInput = socket.openReadChannel()
        }
    }

    fun send(to: String, subject: String, text: String, imgFiles: List<File>) {
        runBlocking {
            var resp = sendAndWait("HELO x\r\n", 220)
            println(resp)

            sendAndWait("AUTH LOGIN\r\n", 250)
            sendAndWait(Base64.getEncoder().encodeToString(creds.username.toByteArray()) + "\r\n", 334)
            sendAndWait(Base64.getEncoder().encodeToString(creds.password.toByteArray()) + "\r\n", 334)
            sendAndWait("MAIL FROM: <$myEmail>\r\n", 235)

            sendMail(to, subject, text, imgFiles.map {it.readBytes()})

            resp = sendAndWait("QUIT\r\n", null)
            println(resp)
        }
    }

    private suspend fun sendAndWait(cmd: String, code: Int?): String {
        myOutput.toOutputStream().write(cmd.toByteArray())
        val resp = myInput.readUTF8Line()
        if (code != null && !resp!!.startsWith(code.toString())) {
            throw IllegalStateException("Server haven't returned $code code: $resp")
        }
        return resp!!
    }

    private fun send(cmd: String) {
        myOutput.toOutputStream().write(cmd.toByteArray())
    }

    suspend fun sendMail(to: String, subject: String, msg: String, imgs: List<ByteArray> = mutableListOf()) {
        println("SENDING MESSAGE")
        sendAndWait("RCPT TO: <$to>\r\n", 250)

        sendAndWait("DATA\r\n", 250)
        sendAndWait("FROM: $myEmail\r\n", 354)
        send("TO: $to\r\n")
        send("SUBJECT: $subject\r\n")
        send("Content-Type: multipart/mixed; boundary=\"!\"\r\n")
        send("\r\n")

        send("--!\r\n")
        send("Content-Type: text/plain;\r\n")
        send("\r\n")
        msg.lines().forEach {line ->
            send("$line\r\n")
        }
        send("\r\n")
        send("--!\r\n")

        imgs.forEach {img ->
            send("Content-Type: image/jpeg;\r\n")
            send("Content-Transfer-Encoding: base64\r\n")
            send("\r\n")
            send(Base64.getEncoder().encodeToString(img))
            send("\r\n")
            send("--!\r\n")
        }
        var resp = sendAndWait(".\r\n", 250)
        println(resp)
    }
}