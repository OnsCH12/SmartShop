package com.example.smartshop.products.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshop.products.data.ProductDatabase
import com.example.smartshop.products.data.ProductRepository
import com.example.smartshop.products.model.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProductRepository

    val allProducts: StateFlow<List<Product>>
    val productCount: StateFlow<Int>
    val totalValue: StateFlow<Double>

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        val productDao = ProductDatabase.getDatabase(application).productDao()
        repository = ProductRepository(productDao)

        allProducts = repository.allProducts
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        productCount = repository.productCount
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)

        totalValue = repository.totalValue
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

        // Listen to real-time Firebase updates
        repository.listenToFirebaseUpdates { products ->
            viewModelScope.launch {
                products.forEach { product ->
                    repository.addProduct(product)
                }
            }
        }

        // Initial sync from Firebase
        syncFromFirebase()
    }

    fun addProduct(name: String, quantity: String, price: String) {
        viewModelScope.launch {
            val error = repository.validateProduct(name, quantity, price)
            if (error != null) {
                _uiState.value = ProductUiState.Error(error)
                return@launch
            }

            _uiState.value = ProductUiState.Loading

            val product = Product(
                name = name,
                quantity = quantity.toInt(),
                price = price.toDouble()
            )

            repository.addProduct(product).fold(
                onSuccess = {
                    _uiState.value = ProductUiState.Success("Produit ajouté avec succès")
                },
                onFailure = { e ->
                    _uiState.value = ProductUiState.Error(e.message ?: "Erreur lors de l'ajout")
                }
            )
        }
    }

    fun updateProduct(product: Product, name: String, quantity: String, price: String) {
        viewModelScope.launch {
            val error = repository.validateProduct(name, quantity, price)
            if (error != null) {
                _uiState.value = ProductUiState.Error(error)
                return@launch
            }

            _uiState.value = ProductUiState.Loading

            val updatedProduct = product.copy(
                name = name,
                quantity = quantity.toInt(),
                price = price.toDouble()
            )

            repository.updateProduct(updatedProduct).fold(
                onSuccess = {
                    _uiState.value = ProductUiState.Success("Produit modifié avec succès")
                },
                onFailure = { e ->
                    _uiState.value = ProductUiState.Error(e.message ?: "Erreur lors de la modification")
                }
            )
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading

            repository.deleteProduct(product).fold(
                onSuccess = {
                    _uiState.value = ProductUiState.Success("Produit supprimé avec succès")
                },
                onFailure = { e ->
                    _uiState.value = ProductUiState.Error(e.message ?: "Erreur lors de la suppression")
                }
            )
        }
    }

    fun syncFromFirebase() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading

            repository.syncFromFirebase().fold(
                onSuccess = {
                    _uiState.value = ProductUiState.Success("Synchronisation réussie")
                },
                onFailure = { e ->
                    _uiState.value = ProductUiState.Error(e.message ?: "Erreur de synchronisation")
                }
            )
        }
    }

    fun resetUiState() {
        _uiState.value = ProductUiState.Idle
    }
}

sealed class ProductUiState {
    object Idle : ProductUiState()
    object Loading : ProductUiState()
    data class Success(val message: String) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}