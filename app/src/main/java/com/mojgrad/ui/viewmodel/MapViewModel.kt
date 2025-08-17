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
        
        db.collection("problems").get()
            .addOnSuccessListener { result ->
                val problemList = result.map { document ->
                    document.toObject(Problem::class.java).copy(id = document.id)
                }
                _problems.value = problemList
                _isLoading.value = false
                println("DEBUG: Loaded ${problemList.size} problems from Firestore")
            }
            .addOnFailureListener { exception ->
                println("DEBUG: Error loading problems: ${exception.message}")
                _isLoading.value = false
                // Obradi grešku
            }
    }
    
    fun refreshProblems() {
        fetchProblems()
    }
}
