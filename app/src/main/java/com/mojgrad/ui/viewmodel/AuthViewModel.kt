package com.mojgrad.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mojgrad.data.model.AuthResult
import com.mojgrad.data.model.User
import com.mojgrad.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        println("DEBUG: AuthViewModel initialized")
        // Prati stanje autentifikacije
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { isLoggedIn ->
                println("DEBUG: Auth state changed: isLoggedIn = $isLoggedIn")
                if (isLoggedIn) {
                    authRepository.currentUser?.let { firebaseUser ->
                        println("DEBUG: Firebase user found: ${firebaseUser.uid}")
                        loadUserData(firebaseUser.uid)
                    } ?: run {
                        println("DEBUG: isLoggedIn=true but currentUser is null")
                    }
                } else {
                    println("DEBUG: User logged out")
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        currentUser = null
                    )
                }
            }
        }
    }

    // Prijava korisnika
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Email i lozinka su obavezni"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    // UI state će biti ažuriran preko getAuthStateFlow()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is AuthResult.Loading -> {
                    // Već je loading = true
                }
            }
        }
    }

    // Registracija korisnika
    fun signUp(email: String, password: String, name: String, phone: String, imageUri: String?) {
        println("DEBUG: AuthViewModel.signUp called with email: $email, imageUri: $imageUri")
        
        if (email.isBlank() || password.isBlank() || name.isBlank() || phone.isBlank()) {
            println("DEBUG: Validation failed - empty fields")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Sva polja su obavezna"
            )
            return
        }

        if (password.length < 6) {
            println("DEBUG: Validation failed - password too short")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Lozinka mora imati najmanje 6 karaktera"
            )
            return
        }

        println("DEBUG: Starting registration process...")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                println("DEBUG: UI state set to loading")

                println("DEBUG: Calling authRepository.signUp...")
                when (val result = authRepository.signUp(email, password, name, phone, imageUri)) {
                    is AuthResult.Success -> {
                        println("DEBUG: Registration successful")
                        // UI state će biti ažuriran preko getAuthStateFlow()
                    }
                    is AuthResult.Error -> {
                        println("DEBUG: Registration failed with error: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    is AuthResult.Loading -> {
                        println("DEBUG: Registration still loading")
                        // Već je loading = true
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Exception in AuthViewModel.signUp: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška tokom registracije: ${e.message}"
                )
            }
        }
    }

    // Učitavanje korisničkih podataka sa retry logikom
    private fun loadUserData(uid: String, retryCount: Int = 0) {
        println("DEBUG: Loading user data for UID: $uid (attempt ${retryCount + 1})")
        viewModelScope.launch {
            when (val result = authRepository.getUserFromFirestore(uid)) {
                is AuthResult.Success -> {
                    println("DEBUG: User data loaded successfully: ${result.data?.name}")
                    println("DEBUG: User is admin: ${result.data?.admin}")
                    println("DEBUG: User image URL: ${result.data?.profileImageUrl}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = result.data,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    println("DEBUG: Failed to load user data: ${result.message}")
                    
                    // Retry up to 3 times with delay for new registrations
                    if (retryCount < 3) {
                        println("DEBUG: Retrying to load user data in 2 seconds (attempt ${retryCount + 2})")
                        delay(2000)
                        loadUserData(uid, retryCount + 1)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true, // Korisnik je i dalje ulogovan u Firebase
                            errorMessage = result.message
                        )
                    }
                }
                is AuthResult.Loading -> {
                    println("DEBUG: Loading user data...")
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    // Odjava korisnika
    fun signOut() {
        authRepository.signOut()
    }

    // Brisanje greške
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
