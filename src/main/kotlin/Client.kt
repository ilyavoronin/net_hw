import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import rawhttp.core.*
import java.net.InetSocketAddress
import java.net.URI

class Client {
    fun run(host: String, port: Int) {
        runBlocking {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(host, port))
            try {
                println("Waiting for connection")
                val input = socket.openReadChannel()
                val output = socket.openWriteChannel(autoFlush = true)
                val connectionMsg = input.readUTF8Line(Int.MAX_VALUE)
                println(connectionMsg)
                if (connectionMsg != "connected") {
                    socket.close()
                    return@runBlocking
                }

                while (true) {
                    println("Input filename:")
                    val fileName = readLine() ?: break
                    if (fileName == "close") {
                        break
                    }

                    val request = RawHttpRequest(
                        RequestLine("GET", URI("/$fileName"), HttpVersion.HTTP_1_1),
                        RawHttpHeaders.newBuilder().with("Host", "$host:$port").build(), null, null)
                    request.writeTo(output.toOutputStream())
                    println(request.toString())

                    var line: String? = null
                    var httpRespString = ""
                    while (line?.isNotEmpty() != false) {
                        line = input.readUTF8Line(Int.MAX_VALUE)
                        httpRespString += "$line" + "\n"
                    }

                    val resp = RawHttp().parseResponse(httpRespString)
                    val bodyLength = resp.headers["Content-Length"]?.toString()?.drop(1)?.dropLast(1)?.toIntOrNull() ?: 0
                    print(httpRespString)
                    println("Body length: $bodyLength")
                    val bytes = ByteArray(bodyLength)
                    (0 until bodyLength).forEach {i ->
                        bytes[i] = input.readByte()
                    }
                    println(bytes.decodeToString())
                }
            } finally {
                socket.close()
            }
        }

    }
}