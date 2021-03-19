
suspend fun main(args: Array<String>) {
    val server = Server(args[0].toInt(), args[1].toInt())
    server.run()
}