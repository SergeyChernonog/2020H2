import domain.Storage
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import protocol.*
import utils.BlockingReceiver
import utils.IoFacade
import java.io.EOFException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

const val DEFAULT_PORT = 8888
const val DEFAULT_ADDRESS = "127.0.0.1"

fun main(args: Array<String>) {

    val parser = ArgParser("server")

    val address by parser.option(
        type = ArgType.String,
        shortName = "a"
    ).default(DEFAULT_ADDRESS)

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p"
    ).default(DEFAULT_PORT)

    parser.parse(args)

    registerMapping(Add::class)
    registerMapping(Buy::class)
    registerMapping(Get::class)
    registerMapping(Supply::class)
    registerMapping(GetProducts::class)

    val server = Server(port, InetAddress.getByName(address))
    while (true) {
        server.handleConnection()
    }

}

class Server(port: Int, address: InetAddress) {
    private val clients = ConcurrentHashMap<String, IoFacade>()
    private val serverSocket = ServerSocket(port, 50, address)
    private val pinger = Executors.newSingleThreadScheduledExecutor()
    private val storage = Storage()

    init {
        serverSocket.receiveBufferSize = BlockingReceiver.bufferSize
        println("Running server on $address:$port")


        storage.supply(storage.addProduct("Pens", 100), 10)
        storage.supply(storage.addProduct("Pencils", 50), 15)
        storage.supply(storage.addProduct("Coins", 1), 678)
        storage.supply(storage.addProduct("Pebbles", 7), 3)
        storage.supply(storage.addProduct("Broomstick handle", 799), 5)
        storage.supply(storage.addProduct("Fishing rod handle", 1699), 9)
        storage.supply(storage.addProduct("Umbrella handle", 99), 1)


        pinger.scheduleAtFixedRate({
            clients.filter {
                try {
                    it.value.ping()
                    true
                } catch (e: Exception) {
                    handleCommunicationException(e, it.key)
                    false
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    fun handleConnection() {
        val socket = serverSocket.accept()
        thread(isDaemon = true) {
            socket.sendBufferSize = BlockingReceiver.bufferSize
            val connection = IoFacade(socket.getInputStream(), socket.getOutputStream())
            val logName = "${socket.inetAddress.hostAddress}:${socket.port}"

            connection.writeMessage(encode(Info("Welcome to our humble shop, merchant!")))
            println("$logName connected")

            try {
                while (true) {
                    handleMessage(connection)
                }
            } catch (e: Exception) {
                handleCommunicationException(e, logName)
            }
        }
    }

    private fun handleMessage(connection: IoFacade) {
        val reply = when (val message = decodeMapped(connection.waitForMessage())) {
            is Add -> handleAdd(message)
            is Get -> handleGet(message)
            is Buy -> handleBuy(message)
            is Supply -> handleSupply(message)
            is GetProducts -> handleGetList()
            else -> handleUnknown()
        }

        connection.writeMessage(reply)
    }

    private fun handleAdd(command: Add) = encode(IntData(storage.addProduct(command.name, command.price)))

    private fun handleGet(command: Get) =
            storage.get(command.id)?.let { encode(ProductDetails(it.product.id, it.product.name, it.product.price, it.amount)) }
                    ?: encode(Error("no product with id ${command.id}"))

    private fun handleBuy(command: Buy) =
            storage.buy(command.id, command.amount)?.let { encode(Info("You can have $it more of this product!")) }
                    ?: encode(Error("can't buy your love and ${command.amount} of ${command.id}"))

    private fun handleSupply(command: Supply) =
            storage.supply(command.id, command.amount)?.let { encode(Info("Now there is $it in stock!")) }
                    ?: encode(Error("looks like you selling air. No product with id ${command.id}"))

    private fun handleGetList() = encode(ProductList(storage.getGoods()))

    private fun handleUnknown() = encode(Error("Unknown message type, sorry uwu"))

    private fun handleCommunicationException(e: Exception, logName: String) {
        when {
            e is SocketException && e.message in okExceptionMessages -> Unit
            e is EOFException -> Unit
            else -> e.printStackTrace()
        }

        handleDisconnect(logName)
    }

    private fun handleDisconnect(logName: String) {
        clients.remove(logName)
        println("$logName disconnected")
    }

    companion object {
        private val okExceptionMessages = listOf(
            "Connection reset",
            "An established connection was aborted by the software in your host machine"
        )
    }
}
