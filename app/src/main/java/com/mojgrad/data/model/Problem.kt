package com.mojgrad.data.model

import com.google.firebase.firestore.GeoPoint

data class Problem(
    val id: String = "",
    val description: String = "",
    val category: String = "",
    val location: GeoPoint? = null,
    val userId: String = ""
)
