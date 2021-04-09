import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SingleThreadDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rawhttp.core.*
import rawhttp.core.body.BytesBody
import rawhttp.core.body.HttpMessageBody
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.net.http.HttpHeaders
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


class Server(port: Int) {
    private val myPort = port
    private val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))

    fun run() {
        runBlocking {
            while (true) {
                val cl = server.accept()
                handle(cl)
            }
        }
    }

    private fun handle(client: Socket) {
        runBlocking {
            val input = client.openReadChannel()
            val output = client.openWriteChannel(autoFlush = true)
            var line: String? = null
            var httpRespString = ""
            while (line?.isNotEmpty() != false) {
                line = input.readUTF8Line(Int.MAX_VALUE)
                httpRespString += "$line" + "\n"
            }
            println("Received request from browser")
            var req = RawHttp().parseRequest(httpRespString)
            print(req.toString())
            val (host, port) = parseHost(req)
            req = modify(req)

            println("Sending request:")
            print(req.toString())
            val connectionSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(host, port))
            val realServerOutput = connectionSocket.openWriteChannel(autoFlush = true)
            val realServerInput = connectionSocket.openReadChannel()



            req.writeTo(realServerOutput.toOutputStream())
            val resp = readResponse(realServerInput)
            println("Received response:")
            println(resp.toString())

            resp.writeTo(output.toOutputStream())
            println("Sending response to browser")
        }
    }

    private suspend fun parsePostBody(resp: HttpMessage, input: ByteReadChannel): ByteArray? {
        val bodyLength = resp.headers["Content-Length"]?.toString()?.drop(1)?.dropLast(1)?.toIntOrNull() ?: return null
        val bytes = ByteArray(bodyLength)
        (0 until bodyLength).forEach {i ->
            bytes[i] = input.readByte()
        }
        return bytes
    }

    private fun parseHost(resp: RawHttpRequest): Pair<String, Int> {
        return resp.uri.path.let {
            val str = it.drop(1)
            val j = str.indexOf(":")
            if (j == -1) {
                Pair(str, 80)
            } else {
                Pair(
                    str.take(j),
                    str.takeLast(str.length - j).toIntOrNull() ?: throw IllegalStateException("Port is not integer")
                )
            }
        }
    }

    suspend fun readResponse(input: ByteReadChannel): RawHttpResponse<Void> {
        var line: String? = null
        var httpRespString = ""
        while (line?.isNotEmpty() != false) {
            line = input.readUTF8Line(Int.MAX_VALUE)
            httpRespString += "$line" + "\n"
        }
        var resp = RawHttp().parseResponse(httpRespString)
        resp = parsePostBody(resp, input)?.let { resp.withBody(BytesBody(it)) }
        return resp
    }

    private fun modify(request: RawHttpRequest): RawHttpRequest {
        var req = RawHttp().parseRequest("GET / HTTP/1.1\nHost: ${request.uri.path.drop(1)}\n")
            .withHeaders(request.headers)
            .withHeaders(RawHttpHeaders.newBuilder()
                .with("Host", request.uri.path.drop(1))
                .with("Connection", "close")
                .build()
            )
        if (req.body.isPresent) {
            req = req.withBody(BytesBody(request.body.get().asRawBytes()))
        }
        return req
    }

}