package com.mojgrad.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
