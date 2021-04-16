import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


class Server(port: Int) {
    private val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))

    private val myFuncs0 = mapOf<String, () -> Int>(
        Pair("func(42)", { 42 }),
        Pair("func(57)", { 57 })
    )
    private val myFuncs1 = mapOf<String, (Int) -> Int>(
        Pair("func(a*5)", { a -> a * 5 }),
    )

    suspend fun run() {
        server.use { server ->
            while (true) {
                val socket = server.accept()
                startNewConnection(socket)
            }
        }
    }

    private suspend fun startNewConnection(socket: Socket) {
        socket.use { socket ->
            println("Waiting for connection")
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true).toOutputStream()
            val type = input.readUTF8Line(Int.MAX_VALUE)
            val cmd = input.readUTF8Line(Int.MAX_VALUE)
            if (type == "f") {
                val line = input.readUTF8Line(Int.MAX_VALUE)!!
                val params = if (line.isBlank()) listOf() else line.split(",").map {it.toInt()}
                if (params.isEmpty()) {
                    if (!myFuncs0.containsKey(cmd)) {
                        output.write("error, no such function with 0 args".toByteArray())
                        return
                    }
                    output.write((myFuncs0[cmd]!!().toString() + "\n").toByteArray())

                } else if (params.size == 1) {
                    if (!myFuncs1.containsKey(cmd)) {
                        output.write("error, no such function with 1 args".toByteArray())
                        return
                    }
                    output.write((myFuncs1[cmd]!!(params[0]).toString() + "\n").toByteArray())
                }
            } else {
                val proc = Runtime.getRuntime().exec(cmd)
                output.write(proc.inputStream.readAllBytes())
                output.write("\n".toByteArray())
            }
        }
    }
}