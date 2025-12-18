package com.example.smartshop.products.data

import com.example.smartshop.products.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductRepository(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private fun getUserProductsCollection() =
        firestore.collection("users").document(userId ?: "").collection("products")

    // Local operations with Flow
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
        .map { entities -> entities.map { Product.fromEntity(it) } }

    val productCount: Flow<Int> = productDao.getProductCount()

    val totalValue: Flow<Double> = productDao.getTotalValue()
        .map { it ?: 0.0 }

    // CRUD operations with Firebase sync
    suspend fun addProduct(product: Product): Result<Product> {
        return try {
            val productId = product.id.ifEmpty { UUID.randomUUID().toString() }
            val newProduct = product.copy(
                id = productId,
                lastModified = System.currentTimeMillis()
            )

            // Save locally
            productDao.insertProduct(newProduct.toEntity())

            // Sync to Firebase
            if (userId != null) {
                getUserProductsCollection()
                    .document(productId)
                    .set(newProduct.toFirestore())
                    .await()
            }

            Result.success(newProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val updatedProduct = product.copy(lastModified = System.currentTimeMillis())

            // Update locally
            productDao.updateProduct(updatedProduct.toEntity())

            // Sync to Firebase
            if (userId != null) {
                getUserProductsCollection()
                    .document(product.id)
                    .set(updatedProduct.toFirestore())
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(product: Product): Result<Unit> {
        return try {
            // Delete locally
            productDao.deleteProduct(product.toEntity())

            // Delete from Firebase
            if (userId != null) {
                getUserProductsCollection()
                    .document(product.id)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync from Firebase to local
    suspend fun syncFromFirebase(): Result<Unit> {
        return try {
            if (userId == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val snapshot = getUserProductsCollection().get().await()
            val products = snapshot.documents.mapNotNull { doc ->
                Product.fromFirestore(doc.id, doc.data ?: return@mapNotNull null)
            }

            // Clear local and insert all from Firebase
            productDao.deleteAllProducts()
            productDao.insertProducts(products.map { it.toEntity() })

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listen to real-time updates from Firebase
    fun listenToFirebaseUpdates(onUpdate: (List<Product>) -> Unit) {
        if (userId == null) return

        getUserProductsCollection().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val products = snapshot.documents.mapNotNull { doc ->
                Product.fromFirestore(doc.id, doc.data ?: return@mapNotNull null)
            }

            onUpdate(products)
        }
    }

    // Validate product data
    fun validateProduct(name: String, quantity: String, price: String): String? {
        if (name.isBlank()) return "Le nom du produit est requis"

        val qty = quantity.toIntOrNull()
        if (qty == null || qty < 0) return "La quantité doit être >= 0"

        val prc = price.toDoubleOrNull()
        if (prc == null || prc <= 0) return "Le prix doit être > 0"

        return null
    }
}