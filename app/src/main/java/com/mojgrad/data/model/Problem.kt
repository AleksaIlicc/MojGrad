package com.mojgrad.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Problem(
    val id: String = "",
    val description: String = "",
    val category: String = "",
    val location: GeoPoint? = null,
    val userId: String = "",
    val authorName: String = "", // Ime i prezime autora
    val votes: Int = 0,
    val status: String = "PRIJAVLJENO",
    @ServerTimestamp
    val timestamp: Date? = null
)
