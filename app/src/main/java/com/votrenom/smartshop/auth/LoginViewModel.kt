package com.example.smartshop.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    init {
        // Check if user is already logged in
        auth.currentUser?.let {
            _loginState.value = LoginState.Success(it.uid)
        }
    }

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
    }

    fun login() {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                if (_email.value.isBlank()) {
                    _loginState.value = LoginState.Error("L'email est requis")
                    return@launch
                }

                if (_password.value.isBlank()) {
                    _loginState.value = LoginState.Error("Le mot de passe est requis")
                    return@launch
                }

                val result = auth.signInWithEmailAndPassword(_email.value, _password.value).await()

                result.user?.let {
                    _loginState.value = LoginState.Success(it.uid)
                } ?: run {
                    _loginState.value = LoginState.Error("Échec de la connexion")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    e.message ?: "Erreur de connexion"
                )
            }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                if (_email.value.isBlank()) {
                    _loginState.value = LoginState.Error("L'email est requis")
                    return@launch
                }

                if (_password.value.length < 6) {
                    _loginState.value = LoginState.Error("Le mot de passe doit contenir au moins 6 caractères")
                    return@launch
                }

                val result = auth.createUserWithEmailAndPassword(_email.value, _password.value).await()

                result.user?.let {
                    _loginState.value = LoginState.Success(it.uid)
                } ?: run {
                    _loginState.value = LoginState.Error("Échec de l'inscription")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    e.message ?: "Erreur d'inscription"
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
        _loginState.value = LoginState.Idle
        _email.value = ""
        _password.value = ""
    }

    fun resetState() {
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
        }
    }
}