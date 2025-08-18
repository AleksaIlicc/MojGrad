package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mojgrad.data.model.Problem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ListaViewModel : ViewModel() {
    private val _problems = MutableStateFlow<List<Problem>>(emptyList())
    val problems: StateFlow<List<Problem>> = _problems
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchProblems()
    }

    private fun fetchProblems() {
        _isLoading.value = true
        _errorMessage.value = null
        val db = FirebaseFirestore.getInstance()
        
        // Koristimo addSnapshotListener za real-time updates sa filterom za aktivne probleme
        db.collection("problems")
            .whereEqualTo("status", "PRIJAVLJENO") // Prikazuj samo aktivne probleme
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _errorMessage.value = "Greška pri učitavanju problema: ${e.message}"
                    _isLoading.value = false
                    println("DEBUG: Error listening to problems: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val problemList = snapshot.documents.map { document ->
                        document.toObject(Problem::class.java)?.copy(id = document.id)
                    }.filterNotNull()
                    
                    _problems.value = problemList
                    _isLoading.value = false
                    println("DEBUG: Lista - Real-time update - Loaded ${problemList.size} active problems")
                } else {
                    println("DEBUG: Lista - No data")
                    _isLoading.value = false
                }
            }
    }
    
    fun refreshProblems() {
        // Sa real-time listener-om, ova funkcija nije potrebna
        // Ali ostavljamo je za kompatibilnost
        println("DEBUG: Lista refresh pozvan - real-time listener već automatski ažurira podatke")
    }
}
