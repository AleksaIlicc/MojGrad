package com.mojgrad.data.model

import com.google.firebase.firestore.GeoPoint

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalPoints: Long = 0L,
    val monthlyPoints: Map<String, Long> = emptyMap(), // Format: "2025-08" -> 320
    val lastLocation: GeoPoint? = null, // Poslednja poznata lokacija korisnika
    val lastLocationUpdate: Long = 0L // Timestamp poslednjeg ažuriranja lokacije
)
