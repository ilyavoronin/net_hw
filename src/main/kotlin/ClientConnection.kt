import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import rawhttp.core.RawHttp
import rawhttp.core.body.FileBody
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class ClientConnection(private val connectionInfo: Server.ConnectionInfo) {
    private var mySocket = AtomicReference<Socket>()

    private val myThread = Thread {
        runBlocking {
            while (true) {
                while (mySocket.get() == null) { }
                runHandler()
                println("connection closed")
                mySocket.set(null)
                connectionInfo.freeConnection(this@ClientConnection)
            }
        }
    }

    init {
        myThread.start()
    }

    fun startNew(socket: Socket) {
        mySocket.set(socket)
    }

    private suspend fun runHandler() {
        val output = mySocket.get().openWriteChannel(autoFlush = true)
        output.toOutputStream().write("connected\n".toByteArray())
        val input = mySocket.get().openReadChannel()
        while (true) {
            try {
                var line: String? = null
                var httpReqString = ""
                while (line?.isNotEmpty() != false) {
                    line = input.readUTF8Line(Int.MAX_VALUE)
                    if (line == null) {
                        return
                    }
                    httpReqString += "$line" + "\n"
                }
                val req = RawHttp().parseRequest(httpReqString)
                val path = req.uri.path
                println("Received query: $path")

                val file = File(path.drop(1))
                val response = if (file.exists()) {
                    RawHttp().parseResponse(
                        "HTTP/1.1 200 OK\n"
                    ).withBody(FileBody(file))
                } else {
                    RawHttp().parseResponse(
                        "HTTP/1.1 404 Not Found\n"
                    )
                }
                response.writeTo(output.toOutputStream());
            } catch (e: Throwable) {
                e.printStackTrace()
                mySocket.get().close()
                break
            }
        }
    }
}