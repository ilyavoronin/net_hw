
fun main(args: Array<String>) {
    val client = Client()

    client.run(args[0], args[1].toInt())
}