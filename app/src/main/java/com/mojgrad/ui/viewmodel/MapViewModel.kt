package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.mojgrad.data.model.Problem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MapViewModel : ViewModel() {
    private val _problems = MutableStateFlow<List<Problem>>(emptyList())
    val problems: StateFlow<List<Problem>> = _problems
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchProblems()
    }

    private fun fetchProblems() {
        _isLoading.value = true
        val db = FirebaseFirestore.getInstance()
        
        // Koristimo addSnapshotListener za real-time updates
        db.collection("problems")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("DEBUG: Error listening to problems: ${e.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val problemList = snapshot.documents.map { document ->
                        document.toObject(Problem::class.java)?.copy(id = document.id)
                    }.filterNotNull()
                    
                    _problems.value = problemList
                    _isLoading.value = false
                    println("DEBUG: Real-time update - Loaded ${problemList.size} problems from Firestore")
                } else {
                    println("DEBUG: No data")
                    _isLoading.value = false
                }
            }
    }
}
