package com.mojgrad.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.firebase.geofire.GeoFireUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.mojgrad.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class AddProblemViewModel(application: Application) : AndroidViewModel(application) {

    enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val locationManager = LocationManager.getInstance(application)

    fun addProblem(description: String, category: String, imageUrl: String? = null) {
        println("DEBUG: AddProblemViewModel - Starting to add problem")
        _uploadState.value = UploadState.UPLOADING


        locationManager.getCurrentLocationOnce { currentLocation ->
            println("DEBUG: AddProblemViewModel - Location callback received: $currentLocation")
            val location = if (currentLocation != null) {
                Location("current").apply {
                    latitude = currentLocation.latitude
                    longitude = currentLocation.longitude
                }
            } else {

                Location("default").apply {
                    latitude = 44.787197
                    longitude = 20.457273
                }
            }

            println("DEBUG: Creating problem at location: ${location.latitude}, ${location.longitude}")
            saveProblemToFirestore(description, category, location, imageUrl)
        }
    }

    private fun saveProblemToFirestore(
        description: String,
        category: String,
        location: Location,
        imageUrl: String? = null
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uploadState.value = UploadState.ERROR
            return
        }


        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val authorName = userSnapshot.getString("name") ?: "Nepoznat korisnik"


                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val geohash = GeoFireUtils.getGeoHashForLocation(
                    com.firebase.geofire.GeoLocation(location.latitude, location.longitude)
                )

                val problem = hashMapOf(
                    "description" to description,
                    "category" to category,
                    "location" to geoPoint,
                    "geohash" to geohash,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "userId" to currentUser.uid,
                    "authorName" to authorName,
                    "status" to "PRIJAVLJENO",
                    "votes" to 0,
                    "imageUrl" to imageUrl
                )

                firestore.collection("problems")
                    .add(problem)
                    .addOnSuccessListener { documentReference ->
                        println("DEBUG: Problem uspešno dodat u Firestore sa ID: ${documentReference.id}")


                        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                        val userRef = firestore.collection("users").document(currentUser.uid)

                        firestore.runTransaction { transaction ->
                            val userSnapshot = transaction.get(userRef)
                            val currentTotalPoints = userSnapshot.getLong("totalPoints") ?: 0L
                            val monthlyPointsMap = userSnapshot.get("monthlyPoints") as? Map<String, Long> ?: emptyMap()
                            val currentMonthPoints = monthlyPointsMap[currentMonth] ?: 0L

                            val updatedMonthlyPoints = monthlyPointsMap.toMutableMap()
                            updatedMonthlyPoints[currentMonth] = currentMonthPoints + 10

                            transaction.update(userRef, mapOf(
                                "totalPoints" to currentTotalPoints + 10,
                                "monthlyPoints" to updatedMonthlyPoints
                            ))
                        }.addOnSuccessListener {
                            println("DEBUG: Uspešno dodeljeno 10 poena korisniku za kreiranje problema")
                        }.addOnFailureListener { e ->
                            println("DEBUG: Greška pri dodeli poena: ${e.message}")
                        }

                        _uploadState.value = UploadState.SUCCESS
                    }
                    .addOnFailureListener { e ->
                        println("DEBUG: Greška pri dodavanju problema: ${e.message}")
                        _uploadState.value = UploadState.ERROR
                    }
            }
            .addOnFailureListener { e ->
                println("DEBUG: Greška pri učitavanju korisničkih podataka: ${e.message}")
                _uploadState.value = UploadState.ERROR
            }
    }

    fun resetState() {
        _uploadState.value = UploadState.IDLE
    }
}
