package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.mojgrad.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class LeaderboardViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val firestore = FirebaseFirestore.getInstance()

    init {
        setupRealtimeLeaderboard()
    }

    private fun getCurrentMonth(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun setupRealtimeLeaderboard() {
        _isLoading.value = true
        _errorMessage.value = null


        firestore.collection("users")
            .whereEqualTo("admin", false)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _errorMessage.value = "Greška pri praćenju rang liste: ${e.message}"
                    _isLoading.value = false
                    println("DEBUG: Greška pri real-time praćenju leaderboard: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val currentMonth = getCurrentMonth()
                    val userList = snapshot.documents.mapNotNull { document ->
                        document.toObject(User::class.java)?.copy(uid = document.id)
                    }
                    .sortedByDescending { user ->

                        user.monthlyPoints[currentMonth] ?: 0
                    }

                    _users.value = userList
                    _isLoading.value = false
                    println("DEBUG: Real-time leaderboard update - Učitano ${userList.size} non-admin korisnika za mesec $currentMonth")
                } else {
                    println("DEBUG: No leaderboard data")
                    _isLoading.value = false
                }
            }
    }

    fun refreshLeaderboard() {


        println("DEBUG: Refresh pozvan - real-time listener već automatski ažurira podatke")
    }
}
