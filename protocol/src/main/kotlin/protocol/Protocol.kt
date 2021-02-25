package protocol


data class IntData(val value: Int) {
    companion object : Parser<IntData> {
        override fun parse(map: Map<String, String>) = IntData(map["value"]!!.toInt())
    }
}

data class ProductDetails(
    val id: Int,
    val name: String,
    val price: Int,
    val amount: Int
) {
    companion object : Parser<ProductDetails> {
        override fun parse(map: Map<String, String>) = ProductDetails(
            map["id"]!!.toInt(),
            map["name"]!!,
            map["price"]!!.toInt(),
            map["amount"]!!.toInt()
        )
    }
}

data class ProductList(val list: String) {
    companion object : Parser<ProductList> {
        override fun parse(map: Map<String, String>): ProductList = ProductList(map["list"]!!)
    }

    constructor(list: List<Product>) : this(list.encodeProducts())
}

data class Error(val message: String) {
    companion object : Parser<Error> {
        override fun parse(map: Map<String, String>) = Error(map["message"]!!)
    }
}

data class Info(val message: String) {
    companion object : Parser<Info> {
        override fun parse(map: Map<String, String>) = Info(map["message"]!!)
    }
}

data class Add(val name: String, val price: Int) {
    companion object : Parser<Add> {
        override fun parse(map: Map<String, String>) = Add(map["name"]!!, map["price"]!!.toInt())
    }
}

data class Supply(val id: Int, val amount: Int) {
    companion object : Parser<Supply> {
        override fun parse(map: Map<String, String>) = Supply(map["id"]!!.toInt(), map["amount"]!!.toInt())
    }
}

data class Buy(val id: Int, val amount: Int) {
    companion object : Parser<Buy> {
        override fun parse(map: Map<String, String>) = Buy(map["id"]!!.toInt(), map["amount"]!!.toInt())
    }
}

data class Get(val id: Int) {
    companion object : Parser<Get> {
        override fun parse(map: Map<String, String>) = Get(map["id"]!!.toInt())
    }
}

class GetProducts {
    companion object : Parser<GetProducts> {
        override fun parse(map: Map<String, String>) = GetProducts()
    }
}