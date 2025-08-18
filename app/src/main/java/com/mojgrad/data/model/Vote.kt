package com.mojgrad.data.model

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Vote(
    val id: String = "",
    val userId: String = "", // Ko je glasao
    val problemId: String = "", // Za koji problem
    @ServerTimestamp
    val votedAt: Date? = null, // Kada je glasao
    val authorId: String = "" // Ko je autor problema (za lakše queries)
)
