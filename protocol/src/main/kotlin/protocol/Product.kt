package protocol

data class Product(
    val id: Int,
    val name: String,
    val price: Int
) {
    companion object {
        fun fromString(string: String) = with(string.split('-')) {
            Product(this[0].toInt(), this[1], this[2].toInt())
        }
    }

    fun toProtocolString() = "$id-$name-$price"

}

fun List<Product>.encodeProducts() = this.joinToString(",") { it.toProtocolString() }

fun String.decodeProducts() = this.split(",").map { Product.fromString(it) }
