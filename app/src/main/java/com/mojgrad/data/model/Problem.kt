package com.mojgrad.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Problem(
    val id: String = "",
    val description: String = "",
    val category: String = "",
    val location: GeoPoint? = null,
    val geohash: String = "",
    val userId: String = "",
    val authorName: String = "",
    val votes: Int = 0,
    val status: String = "PRIJAVLJENO",
    val imageUrl: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)
