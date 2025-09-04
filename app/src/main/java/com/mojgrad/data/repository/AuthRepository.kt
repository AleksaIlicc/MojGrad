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

    fun getAuthStateFlow(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Greška pri prijavi")
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        imageUri: String?
    ): AuthResult<FirebaseUser> {
        return try {
            println("DEBUG: Starting signUp process for email: $email")

            val authResult = withTimeout(30000) {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            val firebaseUser = authResult.user!!
            println("DEBUG: Firebase user created successfully with UID: ${firebaseUser.uid}")


            var profileImageUrl = ""
            if (imageUri != null) {
                // Check if imageUri is already a URL (uploaded from RegistrationScreen)
                if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                    println("DEBUG: Image already uploaded, using provided URL: $imageUri")
                    profileImageUrl = imageUri
                } else {
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
                            }
                        )
                    } catch (e: Exception) {
                        println("DEBUG: Failed to upload profile image: ${e.message}")
                        println("DEBUG: Exception in catch block: ${e::class.java}")
                        e.printStackTrace()
                    }
                }
            }

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

            return AuthResult.Success(firebaseUser)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            println("DEBUG: Signup timeout - mrežni problem")
            return AuthResult.Error("Zahtev je istekao. Proverite internetsku vezu i pokušajte ponovo.")
        } catch (e: Exception) {
            println("DEBUG: SignUp failed with error: ${e.message}")
            println("DEBUG: Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()

            try {
                currentUser?.delete()?.await()
                println("DEBUG: Firebase user deleted after error")
            } catch (deleteError: Exception) {
                println("DEBUG: Failed to delete user: ${deleteError.message}")
            }

            val errorMessage = when {
                e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                    "Email adresa je već registrovana. Molimo pokušajte sa drugim email-om ili se prijavite."
                e.message?.contains("network", ignoreCase = true) == true ||
                e is com.google.firebase.FirebaseNetworkException ->
                    "Mrežna greška. Proverite internetsku vezu i pokušajte ponovo."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Zahtev je istekao. Pokušajte ponovo."
                e.message?.contains("recaptcha", ignoreCase = true) == true ->
                    "Problem sa verifikacijom. Restartujte aplikaciju i pokušajte ponovo."
                else -> e.message ?: "Greška pri registraciji"
            }

            return AuthResult.Error(errorMessage)
        }
    }


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
            throw e
        }
    }


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
