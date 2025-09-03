package com.mojgrad.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val totalPoints: Long = 0L,
    val monthlyPoints: Map<String, Long> = emptyMap(),
    val lastLocation: GeoPoint? = null,
    val lastLocationUpdate: Long = 0L,
    val admin: Boolean = false
)
