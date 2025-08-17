package com.mojgrad.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AddProblemViewModel : ViewModel() {

    enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun addProblem(description: String, category: String) {
        _uploadState.value = UploadState.UPLOADING
        
        // Za početak, koristićemo fiksnu lokaciju (Beograd centar)
        // U budućnosti možemo dodati pravu lokaciju korisnika
        val defaultLocation = Location("default").apply {
            latitude = 44.787197
            longitude = 20.457273
        }
        
        // Direktno čuvanje problema u Firestore bez slike
        saveProblemToFirestore(description, category, defaultLocation)
    }

    private fun saveProblemToFirestore(
        description: String, 
        category: String, 
        location: Location
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uploadState.value = UploadState.ERROR
            return
        }

        val problem = hashMapOf(
            "description" to description,
            "category" to category,
            "location" to GeoPoint(location.latitude, location.longitude),
            "timestamp" to FieldValue.serverTimestamp(),
            "userId" to currentUser.uid,
            "status" to "pending" // Status može biti: pending, in_progress, resolved
        )

        firestore.collection("problems")
            .add(problem)
            .addOnSuccessListener {
                println("DEBUG: Problem uspešno dodat u Firestore sa ID: ${it.id}")
                _uploadState.value = UploadState.SUCCESS
            }
            .addOnFailureListener { e ->
                println("DEBUG: Greška pri dodavanju problema: ${e.message}")
                _uploadState.value = UploadState.ERROR
            }
    }

    fun resetState() {
        _uploadState.value = UploadState.IDLE
    }
}
