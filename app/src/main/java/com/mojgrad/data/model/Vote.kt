package com.mojgrad.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Vote(
    val id: String = "",
    val userId: String = "",
    val problemId: String = "",
    @ServerTimestamp
    val votedAt: Date? = null
)
