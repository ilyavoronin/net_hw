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
                println("Waiting for connection")
                while (true) {
                    val socket =
                        aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(host, port))

                    val input = socket.openReadChannel()
                    val output = socket.openWriteChannel(autoFlush = true).toOutputStream()
                    println("Print command type: f or c")
                    val type = readLine()
                    if (type != "f" && type != "c") {
                        println("Wrong type")
                        continue
                    }
                    output.write((type + "\n").toByteArray())
                    if (type == "f") {
                        println("Insert function name:")
                        val fname = readLine() + "\n"
                        println("Insert params separated with `,`")
                        val params = readLine() + "\n"
                        output.write(fname.toByteArray())
                        output.write(params.toByteArray())
                    } else {
                        println("Insert command:")
                        val cname = readLine() + "\n"
                        output.write(cname.toByteArray())
                    }
                    val resp = input.readUTF8Line(Int.MAX_VALUE)
                    println("Response: $resp")
                }
        }

    }
}