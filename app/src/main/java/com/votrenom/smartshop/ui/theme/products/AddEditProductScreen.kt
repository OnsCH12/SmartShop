package com.example.smartshop.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshop.products.model.Product
import com.example.smartshop.products.viewmodel.ProductUiState
import com.example.smartshop.products.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ProductViewModel,
    productId: String?,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val product = remember(productId, products) {
        productId?.let { id -> products.find { it.id == id } }
    }

    var name by remember { mutableStateOf(product?.name ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }

    val isEditMode = product != null
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProductUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                onNavigateBack()
            }
            is ProductUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modifier le produit" else "Ajouter un produit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du produit") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is ProductUiState.Loading
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantité") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = uiState !is ProductUiState.Loading,
                supportingText = { Text("Doit être >= 0") }
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Prix (DT)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = uiState !is ProductUiState.Loading,
                supportingText = { Text("Doit être > 0") }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isEditMode && product != null) {
                        viewModel.updateProduct(product, name, quantity, price)
                    } else {
                        viewModel.addProduct(name, quantity, price)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is ProductUiState.Loading
            ) {
                if (uiState is ProductUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditMode) "Modifier" else "Ajouter")
                }
            }
        }
    }
}