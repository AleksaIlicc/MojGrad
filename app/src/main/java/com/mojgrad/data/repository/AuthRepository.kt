package com.mojgrad.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mojgrad.data.model.AuthResult
import com.mojgrad.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    // Flow za praćenje stanja autentifikacije
    fun getAuthStateFlow(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    // Prijava korisnika
    suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Greška pri prijavi")
        }
    }

    // Registracija korisnika
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        imageUri: Uri?
    ): AuthResult<FirebaseUser> {
        return try {
            println("DEBUG: Starting signUp process for email: $email")
            
            // 1. Kreiranje korisnika u Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!
            println("DEBUG: Firebase user created successfully with UID: ${firebaseUser.uid}")

            // 2. Čuvanje korisničkih podataka u Firestore (bez slike)
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                phoneNumber = phone,
                profileImageUrl = "" // Uvek prazan string
            )
            println("DEBUG: User object created, saving to Firestore...")

            saveUserToFirestore(user)
            println("DEBUG: User saved to Firestore successfully")

            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            println("DEBUG: SignUp failed with error: ${e.message}")
            println("DEBUG: Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            
            // Ako se desila greška nakon kreiranja korisnika, obriši ga
            try {
                currentUser?.delete()?.await()
                println("DEBUG: Firebase user deleted after error")
            } catch (deleteError: Exception) {
                println("DEBUG: Failed to delete user: ${deleteError.message}")
            }
            
            AuthResult.Error(e.message ?: "Greška pri registraciji")
        }
    }

    // Čuvanje korisnika u Firestore
    private suspend fun saveUserToFirestore(user: User) {
        try {
            println("DEBUG: Saving user to Firestore: ${user.uid}")
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            println("DEBUG: Firestore save successful")
        } catch (e: Exception) {
            println("DEBUG: Firestore save failed: ${e.message}")
            throw e // Re-throw da se pošalje dalje u signUp
        }
    }

    // Dohvatanje korisnika iz Firestore
    suspend fun getUserFromFirestore(uid: String): AuthResult<User> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (document.exists()) {
                val user = document.toObject(User::class.java)!!
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Korisnik nije pronađen")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Greška pri dohvatanju korisnika")
        }
    }

    // Odjava korisnika
    fun signOut() {
        auth.signOut()
    }
}
