package com.example.smartshop.products.model

data class Product(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val lastModified: Long = System.currentTimeMillis()
) {
    fun toEntity() = ProductEntity(
        id = id,
        name = name,
        quantity = quantity,
        price = price,
        lastModified = lastModified
    )

    companion object {
        fun fromEntity(entity: ProductEntity) = Product(
            id = entity.id,
            name = entity.name,
            quantity = entity.quantity,
            price = entity.price,
            lastModified = entity.lastModified
        )

        fun fromFirestore(id: String, data: Map<String, Any>) = Product(
            id = id,
            name = data["name"] as? String ?: "",
            quantity = (data["quantity"] as? Long)?.toInt() ?: 0,
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            lastModified = data["lastModified"] as? Long ?: System.currentTimeMillis()
        )
    }

    fun toFirestore() = hashMapOf(
        "name" to name,
        "quantity" to quantity,
        "price" to price,
        "lastModified" to lastModified
    )
}