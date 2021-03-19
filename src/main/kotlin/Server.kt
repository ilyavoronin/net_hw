import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


class Server(port: Int, private val maxThreads: Int) {
    private val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))
    private val connectionInfo = ConnectionInfo()

    suspend fun run() {
        server.use { server ->
            while (true) {
                val socket = server.accept()
                println("new connection request")
                startNewConnection(socket)
            }
        }
    }

    private suspend fun startNewConnection(socket: Socket) {
        val connection = connectionInfo.getConnection()
        connection.startNew(socket)
    }

    inner class ConnectionInfo {
        private val clientConnections = mutableListOf<ClientConnection>()
        private val freeConnections =  Channel<ClientConnection>()

        suspend fun getConnection(): ClientConnection {
            return if (clientConnections.size < maxThreads) {
                val newConnection = ClientConnection(connectionInfo)
                clientConnections.add(newConnection)
                newConnection
            } else {
                awaitConnection()
            }
        }

        fun freeConnection(connection: ClientConnection) {
            runBlocking {
                freeConnections.send(connection)
            }
        }

        private suspend fun awaitConnection(): ClientConnection {
            return freeConnections.receive()
        }
    }
}