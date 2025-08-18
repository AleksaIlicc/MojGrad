package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mojgrad.data.model.Problem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class ProblemDetailsViewModel : ViewModel() {
    
    enum class VoteState { IDLE, VOTING, SUCCESS, ERROR, ALREADY_VOTED, SELF_VOTE }
    
    private val _problem = MutableStateFlow<Problem?>(null)
    val problem: StateFlow<Problem?> = _problem
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _voteState = MutableStateFlow(VoteState.IDLE)
    val voteState: StateFlow<VoteState> = _voteState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _hasUserVoted = MutableStateFlow(false)
    val hasUserVoted: StateFlow<Boolean> = _hasUserVoted
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    fun loadProblem(problemId: String) {
        _isLoading.value = true
        
        firestore.collection("problems")
            .document(problemId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val problem = document.toObject(Problem::class.java)?.copy(id = document.id)
                    _problem.value = problem
                    println("DEBUG: Problem učitan - ${problem?.description} sa ${problem?.votes} glasova")
                    
                    // Proverava da li je trenutni korisnik glasao
                    checkIfUserVoted(problemId)
                } else {
                    _errorMessage.value = "Problem nije pronađen"
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Greška pri učitavanju: ${e.message}"
                _isLoading.value = false
            }
    }
    
    private fun checkIfUserVoted(problemId: String) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            _hasUserVoted.value = false
            return
        }
        
        firestore.collection("votes")
            .whereEqualTo("userId", currentUserUid)
            .whereEqualTo("problemId", problemId)
            .get()
            .addOnSuccessListener { result ->
                _hasUserVoted.value = !result.isEmpty
                println("DEBUG: Korisnik ${if (_hasUserVoted.value) "jeste" else "nije"} glasao za problem $problemId")
            }
            .addOnFailureListener { e ->
                println("DEBUG: Greška pri proveri glasanja: ${e.message}")
                _hasUserVoted.value = false
            }
    }
    
    fun voteForProblem(problemId: String, authorId: String) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            _voteState.value = VoteState.ERROR
            _errorMessage.value = "Morate biti ulogovani da biste glasali"
            return
        }
        
        // Ne dozvoli korisniku da glasa za sopstveni problem
        if (currentUserUid == authorId) {
            _voteState.value = VoteState.SELF_VOTE
            _errorMessage.value = "Ne možete glasati za sopstveni problem"
            return
        }
        
        _voteState.value = VoteState.VOTING
        
        // Proverava da li je korisnik već glasao - koristi votes kolekciju
        firestore.collection("votes")
            .whereEqualTo("userId", currentUserUid)
            .whereEqualTo("problemId", problemId)
            .get()
            .addOnSuccessListener { existingVotes ->
                if (!existingVotes.isEmpty) {
                    // Korisnik je već glasao
                    _voteState.value = VoteState.ALREADY_VOTED
                    _errorMessage.value = "Već ste glasali za ovaj problem"
                    return@addOnSuccessListener
                }
                
                // Korisnik nije glasao, nastavi sa glasanjem
                performVoteTransaction(problemId, authorId, currentUserUid)
            }
            .addOnFailureListener { e ->
                _voteState.value = VoteState.ERROR
                _errorMessage.value = "Greška pri proveri glasanja: ${e.message}"
            }
    }
    
    private fun performVoteTransaction(problemId: String, authorId: String, voterId: String) {
        val problemRef = firestore.collection("problems").document(problemId)
        val voterUserRef = firestore.collection("users").document(voterId)
        val authorUserRef = firestore.collection("users").document(authorId)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        
        firestore.runTransaction { transaction ->
            // 1. Povećaj broj glasova na problemu
            transaction.update(problemRef, "votes", FieldValue.increment(1))
            
            // 2. Dodeli poene korisniku koji glasa (+1 poen)
            val voterSnapshot = transaction.get(voterUserRef)
            val voterTotalPoints = voterSnapshot.getLong("totalPoints") ?: 0L
            val voterMonthlyPoints = voterSnapshot.get("monthlyPoints") as? Map<String, Long> ?: emptyMap()
            val voterCurrentMonthPoints = voterMonthlyPoints[currentMonth] ?: 0L
            
            val updatedVoterMonthlyPoints = voterMonthlyPoints.toMutableMap()
            updatedVoterMonthlyPoints[currentMonth] = voterCurrentMonthPoints + 1
            
            transaction.update(voterUserRef, mapOf(
                "totalPoints" to voterTotalPoints + 1,
                "monthlyPoints" to updatedVoterMonthlyPoints
            ))
            
            // 3. Dodeli poene autoru problema (+1 poena za dobijanje glasa)
            val authorSnapshot = transaction.get(authorUserRef)
            val authorTotalPoints = authorSnapshot.getLong("totalPoints") ?: 0L
            val authorMonthlyPoints = authorSnapshot.get("monthlyPoints") as? Map<String, Long> ?: emptyMap()
            val authorCurrentMonthPoints = authorMonthlyPoints[currentMonth] ?: 0L
            
            val updatedAuthorMonthlyPoints = authorMonthlyPoints.toMutableMap()
            updatedAuthorMonthlyPoints[currentMonth] = authorCurrentMonthPoints + 1
            
            transaction.update(authorUserRef, mapOf(
                "totalPoints" to authorTotalPoints + 1,
                "monthlyPoints" to updatedAuthorMonthlyPoints
            ))
            
            null
        }.addOnSuccessListener {
            // 4. Dodaj vote record u votes kolekciju (van transakcije jer nije kritično)
            val voteData = hashMapOf(
                "userId" to voterId,
                "problemId" to problemId,
                "authorId" to authorId,
                "votedAt" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("votes")
                .add(voteData)
                .addOnSuccessListener {
                    println("DEBUG: Vote record kreiran sa ID: ${it.id}")
                }
                .addOnFailureListener { e ->
                    println("DEBUG: Greška pri kreiranju vote record: ${e.message}")
                    // Ne prekidamo proces jer su glavne operacije uspešne
                }
            
            _voteState.value = VoteState.SUCCESS
            println("DEBUG: Uspešno glasanje za problem $problemId")
            
            // Osvežaj problem da prikažemo ažurirani broj glasova
            loadProblem(problemId)
        }.addOnFailureListener { e ->
            _voteState.value = VoteState.ERROR
            _errorMessage.value = "Greška pri glasanju: ${e.message}"
            println("DEBUG: Greška pri glasanju: ${e.message}")
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
        _voteState.value = VoteState.IDLE
    }
}
