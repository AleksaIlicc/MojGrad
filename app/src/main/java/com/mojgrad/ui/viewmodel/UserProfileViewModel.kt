package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mojgrad.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserProfileViewModel : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var userListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        setupAuthListener()
    }

    private fun setupAuthListener() {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {

                loadUserProfile(currentUser.uid)
                println("DEBUG: UserProfileViewModel - Novi korisnik ulogovan: ${currentUser.uid}")
            } else {

                clearUserData()
                println("DEBUG: UserProfileViewModel - Korisnik se izlogovao")
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun clearUserData() {
        userListener?.remove()
        userListener = null
        _currentUser.value = null
        _isLoading.value = false
        _errorMessage.value = null
    }

    private fun loadUserProfile(userId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        println("DEBUG: UserProfileViewModel - Attempting to load user profile for: $userId")


        userListener?.remove()


        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _errorMessage.value = "Greška pri učitavanju profila: ${e.message}"
                    _isLoading.value = false
                    println("DEBUG: UserProfileViewModel - Greška pri učitavanju profila: ${e.message}")
                    return@addSnapshotListener
                }

                println("DEBUG: UserProfileViewModel - Firestore snapshot received for $userId")
                println("DEBUG: UserProfileViewModel - Snapshot exists: ${snapshot?.exists()}")
                println("DEBUG: UserProfileViewModel - Snapshot data: ${snapshot?.data}")

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
                    _currentUser.value = user
                    println("DEBUG: UserProfileViewModel - Učitan korisnik: ${user?.name}, poeni: ${user?.totalPoints}")
                } else {
                    _errorMessage.value = "Profil korisnika nije pronađen u Firestore bazi"
                    println("DEBUG: UserProfileViewModel - Profil korisnika $userId nije pronađen u Firestore")
                    println("DEBUG: UserProfileViewModel - PROBLEM: Korisnik postoji u Authentication ali se nije sačuvao u Firestore tijekom registracije!")
                    println("DEBUG: UserProfileViewModel - Provjerite da li se AuthRepository.saveUserToFirestore() poziva uspješno")
                }
                _isLoading.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        authListener?.let { auth.removeAuthStateListener(it) }
    }

    fun signOut() {
        auth.signOut()
    }
}
