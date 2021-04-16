
suspend fun main() {
    val port = 4040
    val server = Server(port)
    server.run()
}