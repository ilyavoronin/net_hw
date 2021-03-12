import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import rawhttp.core.RawHttp
import rawhttp.core.body.FileBody
import java.io.File
import java.net.InetSocketAddress


class Server(port: Int) {
    private val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))

    fun run() {
        println("Listening on: ${server.localAddress}")
        runBlocking {
            val socket = server.accept()
            val output = socket.openWriteChannel(autoFlush = true)
            val input = socket.openReadChannel()
            try {
                var line: String? = null
                var httpReqString = ""
                while (line?.isNotEmpty() != false) {
                    line = input.readUTF8Line(Int.MAX_VALUE)
                    httpReqString += "$line" + "\n"
                }
                val req = RawHttp().parseRequest(httpReqString)
                val path = req.uri.path
                println("Received query: $path")

                val file = File(path.drop(1))
                val response = if (file.exists()) {
                    println("Ok")
                    RawHttp().parseResponse(
                        "HTTP/1.1 200 OK\n"
                    ).withBody(FileBody(file))
                } else {
                    println("Not found")
                    RawHttp().parseResponse(
                        "HTTP/1.1 404 Not Found\n"
                    )
                }
                response.writeTo(output.toOutputStream());
            } catch (e: Throwable) {
                e.printStackTrace()
                socket.close()
            }
        }
    }
}