package com.mojgrad.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mojgrad.data.model.AuthResult
import com.mojgrad.data.model.User
import com.mojgrad.service.ImageUploadService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val imageUploadService = ImageUploadService(context)

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
        imageUri: String?
    ): AuthResult<FirebaseUser> {
        return try {
            println("DEBUG: Starting signUp process for email: $email")
            
            // 1. Kreiranje korisnika u Firebase Auth sa timeout-om
            val authResult = withTimeout(30000) { // 30 sekundi timeout
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            val firebaseUser = authResult.user!!
            println("DEBUG: Firebase user created successfully with UID: ${firebaseUser.uid}")

            // 2. Handle profile image
            var profileImageUrl = ""
            if (imageUri != null) {
                // Check if imageUri is already a URL (uploaded from RegistrationScreen)
                if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                    println("DEBUG: Image already uploaded, using provided URL: $imageUri")
                    profileImageUrl = imageUri
                } else {
                    // It's a local URI, need to upload
                    try {
                        println("DEBUG: Uploading profile image to R2...")
                        val uploadResult = imageUploadService.uploadImage(Uri.parse(imageUri))
                        println("DEBUG: Upload result received: $uploadResult")
                        uploadResult.fold(
                            onSuccess = { uploadResponse ->
                                profileImageUrl = uploadResponse.url
                                println("DEBUG: Profile image uploaded successfully: $profileImageUrl")
                                println("DEBUG: Upload response: key=${uploadResponse.key}, size=${uploadResponse.size}")
                            },
                            onFailure = { exception ->
                                println("DEBUG: Failed to upload profile image: ${exception.message}")
                                println("DEBUG: Exception type: ${exception::class.java}")
                                exception.printStackTrace()
                                // Continue without image rather than failing registration
                            }
                        )
                    } catch (e: Exception) {
                        println("DEBUG: Failed to upload profile image: ${e.message}")
                        println("DEBUG: Exception in catch block: ${e::class.java}")
                        e.printStackTrace()
                        // Continue without image rather than failing registration
                    }
                }
            }

            // 3. Čuvanje korisničkih podataka u Firestore sa slikom
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                phoneNumber = phone,
                profileImageUrl = profileImageUrl
            )
            println("DEBUG: User object created with profileImageUrl: $profileImageUrl")

            try {
                saveUserToFirestore(user)
                println("DEBUG: User saved to Firestore successfully")
            } catch (e: Exception) {
                println("DEBUG: CRITICAL ERROR - Failed to save user to Firestore: ${e.message}")
                // Pokušaj ponovno bez slike ako je problem sa slikom
                if (profileImageUrl.isNotEmpty()) {
                    println("DEBUG: Retrying save without profile image...")
                    val userWithoutImage = user.copy(profileImageUrl = "")
                    try {
                        saveUserToFirestore(userWithoutImage)
                        println("DEBUG: User saved to Firestore successfully (without image)")
                    } catch (e2: Exception) {
                        println("DEBUG: FATAL ERROR - Cannot save user to Firestore even without image: ${e2.message}")
                        throw e2
                    }
                } else {
                    throw e
                }
            }

            AuthResult.Success(firebaseUser)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            println("DEBUG: Signup timeout - mrežni problem")
            AuthResult.Error("Zahtev je istekao. Proverite internetsku vezu i pokušajte ponovo.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            println("DEBUG: Firebase network exception - trying alternative approach")
            // Pokušaj sa emulator auth bypass-om
            tryAlternativeSignup(email, password, name, phone)
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
            
            val errorMessage = when {
                e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                    "Email adresa je već registrovana. Molimo pokušajte sa drugim email-om ili se prijavite."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Mrežna greška. Proverite internetsku vezu i pokušajte ponovo."
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Zahtev je istekao. Pokušajte ponovo."
                e.message?.contains("recaptcha", ignoreCase = true) == true ->
                    "Problem sa verifikacijom. Restartujte aplikaciju i pokušajte ponovo."
                e is com.google.firebase.FirebaseNetworkException ->
                    "Mrežna greška. Pokušajte kasnije ili proverite internetsku vezu."
                else -> e.message ?: "Greška pri registraciji"
            }
            
            AuthResult.Error(errorMessage)
        }
    }
    
    // Alternativni signup za development kada ima mrežnih problema
    private suspend fun tryAlternativeSignup(
        email: String,
        password: String,
        name: String,
        phone: String
    ): AuthResult<FirebaseUser> {
        return try {
            println("DEBUG: Trying alternative signup approach")
            
            // Pokušaj bez timeout-a i sa kraćim pristupom
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Korisnik nije kreiran")
            println("DEBUG: Alternative signup successful: ${firebaseUser.uid}")

            // Kreiranje User objekta
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                phoneNumber = phone,
                profileImageUrl = ""
            )

            // Čuvanje u Firestore
            saveUserToFirestore(user)

            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            println("DEBUG: Alternative signup also failed: ${e.message}")
            AuthResult.Error("Neuspešna registracija. Molimo pokušajte kasnije.")
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
    
    fun signOut() {
        auth.signOut()
    }
}
