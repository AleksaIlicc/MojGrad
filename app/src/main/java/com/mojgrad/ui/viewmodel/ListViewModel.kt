package com.mojgrad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mojgrad.data.model.Problem
import com.mojgrad.data.model.Vote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListViewModel : ViewModel() {
    private val _allProblems = MutableStateFlow<List<Problem>>(emptyList())
    private val _problems = MutableStateFlow<List<Problem>>(emptyList())
    val problems: StateFlow<List<Problem>> = _problems
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    // Praćenje glasova korisnika za probleme (problemId -> hasVoted)
    private val _userVotes = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val userVotes: StateFlow<Map<String, Boolean>> = _userVotes
    
    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory
    
    private val _selectedAuthor = MutableStateFlow<String?>(null)
    val selectedAuthor: StateFlow<String?> = _selectedAuthor
    
    private val _selectedStatus = MutableStateFlow("PRIJAVLJENO")
    val selectedStatus: StateFlow<String> = _selectedStatus
    
    private val _sortBy = MutableStateFlow("newest") // "newest", "votes"
    val sortBy: StateFlow<String> = _sortBy
    
    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateRange: StateFlow<Pair<Long?, Long?>> = _dateRange
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        fetchProblems()
        fetchUserVotes()
        // Apply filters whenever any filter changes
        observeFilters()
    }
    
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                _allProblems,
                _searchQuery,
                _selectedCategory,
                _selectedAuthor,
                _selectedStatus,
                _sortBy,
                _dateRange
            ) { flows ->
                val allProblems = flows[0] as List<Problem>
                val searchQuery = flows[1] as String
                val category = flows[2] as String?
                val author = flows[3] as String?
                val status = flows[4] as String
                val sortBy = flows[5] as String
                val dateRange = flows[6] as Pair<Long?, Long?>
                
                applyFilters(allProblems, searchQuery, category, author, status, sortBy, dateRange)
            }.collect { filteredProblems ->
                _problems.value = filteredProblems
            }
        }
    }

    private fun fetchProblems() {
        _isLoading.value = true
        _errorMessage.value = null
        
        // Fetch ALL problems for better filtering/searching
        db.collection("problems")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _errorMessage.value = "Error loading problems: ${e.message}"
                    _isLoading.value = false
                    println("DEBUG: Error listening to problems: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val problemList = snapshot.documents.map { document ->
                        document.toObject(Problem::class.java)?.copy(id = document.id)
                    }.filterNotNull()
                    
                    _allProblems.value = problemList
                    _isLoading.value = false
                    println("DEBUG: ListViewModel - Real-time update - Loaded ${problemList.size} total problems")
                } else {
                    println("DEBUG: ListViewModel - No data")
                    _isLoading.value = false
                }
            }
    }
    
    private fun fetchUserVotes() {
        val currentUser = auth.currentUser 
        if (currentUser == null) {
            println("DEBUG: No current user for fetching votes")
            return
        }
        
        println("DEBUG: Fetching votes for user: ${currentUser.uid}")
        
        // Real-time listener za glasove korisnika
        db.collection("votes")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("DEBUG: Error listening to user votes: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val votesMap = snapshot.documents.mapNotNull { document ->
                        val vote = document.toObject(Vote::class.java)
                        vote?.problemId
                    }.associateWith { true }
                    
                    _userVotes.value = votesMap
                    println("DEBUG: User votes updated - ${votesMap.size} votes: $votesMap")
                } else {
                    println("DEBUG: No vote snapshot data")
                }
            }
    }
    
    private fun applyFilters(
        allProblems: List<Problem>,
        searchQuery: String,
        category: String?,
        author: String?,
        status: String,
        sortBy: String,
        dateRange: Pair<Long?, Long?>
    ): List<Problem> {
        return allProblems
            .filter { problem ->
                // Status filter
                problem.status == status
            }
            .filter { problem ->
                // Search filter (description and category)
                if (searchQuery.isBlank()) true
                else {
                    problem.description.contains(searchQuery, ignoreCase = true) ||
                    problem.category.contains(searchQuery, ignoreCase = true)
                }
            }
            .filter { problem ->
                // Category filter
                category?.let { problem.category == it } ?: true
            }
            .filter { problem ->
                // Author filter
                author?.let { problem.authorName == it } ?: true
            }
            .filter { problem ->
                // Date range filter
                val (startDate, endDate) = dateRange
                when {
                    startDate != null && endDate != null && problem.timestamp != null -> {
                        val problemTime = problem.timestamp!!.time
                        problemTime >= startDate && problemTime <= endDate
                    }
                    startDate != null && problem.timestamp != null -> {
                        problem.timestamp!!.time >= startDate
                    }
                    endDate != null && problem.timestamp != null -> {
                        problem.timestamp!!.time <= endDate
                    }
                    else -> true
                }
            }
            .let { filteredList ->
                // Sort
                when (sortBy) {
                    "votes" -> filteredList.sortedByDescending { it.votes }
                    "newest" -> filteredList.sortedByDescending { it.timestamp }
                    else -> filteredList.sortedByDescending { it.timestamp }
                }
            }
    }
    
    // Filter update functions
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }
    
    fun updateAuthorFilter(author: String?) {
        _selectedAuthor.value = author
    }
    
    fun updateStatusFilter(status: String) {
        _selectedStatus.value = status
    }
    
    fun updateSortBy(sortBy: String) {
        _sortBy.value = sortBy
    }
    
    fun updateDateRange(startDate: Long?, endDate: Long?) {
        _dateRange.value = startDate to endDate
    }
    
    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedAuthor.value = null
        _selectedStatus.value = "PRIJAVLJENO"
        _sortBy.value = "newest"
        _dateRange.value = null to null
    }
    
    // Get unique values for filter options
    fun getUniqueCategories(): List<String> {
        return _allProblems.value.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    fun getUniqueAuthors(): List<String> {
        return _allProblems.value.map { it.authorName }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    fun toggleVoteForProblem(problem: Problem) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "Morate biti ulogovani da biste glasali"
            println("DEBUG: User not logged in for voting")
            return
        }
        
        // Sprečavanje glasanja za sopstvene probleme
        if (problem.userId == currentUser.uid) {
            _errorMessage.value = "Ne možete glasati za sopstvene probleme"
            println("DEBUG: User trying to vote for own problem")
            return
        }
        
        viewModelScope.launch {
            try {
                val hasVoted = _userVotes.value[problem.id] == true
                println("DEBUG: Toggle vote for problem ${problem.id}, hasVoted: $hasVoted")
                
                if (hasVoted) {
                    // Remove vote
                    println("DEBUG: Removing vote for problem ${problem.id}")
                    removeVote(problem.id, currentUser.uid)
                } else {
                    // Add vote
                    println("DEBUG: Adding vote for problem ${problem.id}")
                    addVote(problem.id, currentUser.uid)
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Greška prilikom glasanja: ${e.message}"
                println("DEBUG: Error toggling vote: ${e.message}")
            }
        }
    }
    
    private suspend fun addVote(problemId: String, userId: String) {
        // Atomska transakcija za sve vote operacije
        db.runTransaction { transaction ->
            // === FAZA 1: SVI READ-OVI MORAJU BITI PRVI ===
            
            // 1. Čita problem dokument
            val problemRef = db.collection("problems").document(problemId)
            val problemSnapshot = transaction.get(problemRef)
            
            // 2. Čita postojeći vote dokument
            val voteId = "${userId}_${problemId}"
            val voteRef = db.collection("votes").document(voteId)
            val existingVoteSnapshot = transaction.get(voteRef)
            
            // 3. Čita glasač dokument
            val voterRef = db.collection("users").document(userId)
            val voterSnapshot = transaction.get(voterRef)
            
            // 4. Čita autor problema dokument
            val problemAuthorId = problemSnapshot.getString("userId") 
                ?: throw Exception("Problem nema autora")
            val authorRef = db.collection("users").document(problemAuthorId)
            val authorSnapshot = transaction.get(authorRef)
            
            // === FAZA 2: VALIDACIJA ===
            
            if (!problemSnapshot.exists()) {
                throw Exception("Problem više ne postoji")
            }
            
            if (problemAuthorId == userId) {
                throw Exception("Ne možete glasati za sopstvene probleme")
            }
            
            if (existingVoteSnapshot.exists()) {
                throw Exception("Već ste glasali za ovaj problem")
            }
            
            // === FAZA 3: SVI WRITE-OVI POSLE SVIH READ-OVA ===
            
            // 5. Kreira vote dokument
            val vote = Vote(
                id = voteId,
                userId = userId,
                problemId = problemId
            )
            transaction.set(voteRef, vote)
            
            // 6. Ažurira broj glasova na problemu
            val currentVotes = problemSnapshot.getLong("votes") ?: 0L
            transaction.update(problemRef, "votes", currentVotes + 1)
            
            // 7. Dodaje poene glasaču
            if (voterSnapshot.exists()) {
                updateUserPointsInTransaction(transaction, voterRef, voterSnapshot, 1)
            }
            
            // 8. Dodaje poene autoru problema
            if (authorSnapshot.exists()) {
                updateUserPointsInTransaction(transaction, authorRef, authorSnapshot, 1)
            }
            
            problemAuthorId // Vraća author ID za logging
        }.await()
        
        println("DEBUG: Vote added for problem $problemId by user $userId")
    }
    
    private suspend fun removeVote(problemId: String, userId: String) {
        // Atomska transakcija za uklanjanje vote-a
        db.runTransaction { transaction ->
            // === FAZA 1: SVI READ-OVI MORAJU BITI PRVI ===
            
            // 1. Čita postojeći vote dokument
            val voteId = "${userId}_${problemId}"
            val voteRef = db.collection("votes").document(voteId)
            val voteSnapshot = transaction.get(voteRef)
            
            // 2. Čita problem dokument
            val problemRef = db.collection("problems").document(problemId)
            val problemSnapshot = transaction.get(problemRef)
            
            // 3. Čita glasač dokument
            val voterRef = db.collection("users").document(userId)
            val voterSnapshot = transaction.get(voterRef)
            
            // 4. Čita autor problema dokument
            val problemAuthorId = problemSnapshot.getString("userId")
                ?: throw Exception("Problem nema autora")
            val authorRef = db.collection("users").document(problemAuthorId)
            val authorSnapshot = transaction.get(authorRef)
            
            // === FAZA 2: VALIDACIJA ===
            
            if (!voteSnapshot.exists()) {
                throw Exception("Vote ne postoji")
            }
            
            if (!problemSnapshot.exists()) {
                throw Exception("Problem više ne postoji")
            }
            
            // === FAZA 3: SVI WRITE-OVI POSLE SVIH READ-OVA ===
            
            // 5. Briše vote dokument
            transaction.delete(voteRef)
            
            // 6. Smanjuje broj glasova na problemu (ali ne ispod 0)
            val currentVotes = problemSnapshot.getLong("votes") ?: 0L
            val newVotes = maxOf(0L, currentVotes - 1)
            transaction.update(problemRef, "votes", newVotes)
            
            // 7. Oduzima poene glasaču
            if (voterSnapshot.exists()) {
                updateUserPointsInTransaction(transaction, voterRef, voterSnapshot, -1)
            }
            
            // 8. Oduzima poene autoru problema
            if (authorSnapshot.exists()) {
                updateUserPointsInTransaction(transaction, authorRef, authorSnapshot, -1)
            }
            
            problemAuthorId // Vraća author ID za logging
        }.await()
        
        println("DEBUG: Vote removed for problem $problemId by user $userId")
    }
    
    private fun updateUserPointsInTransaction(
        transaction: com.google.firebase.firestore.Transaction,
        userRef: com.google.firebase.firestore.DocumentReference,
        userSnapshot: com.google.firebase.firestore.DocumentSnapshot,
        pointsChange: Long
    ) {
        val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        
        val currentTotalPoints = userSnapshot.getLong("totalPoints") ?: 0L
        val monthlyPointsMap = userSnapshot.get("monthlyPoints") as? Map<String, Long> ?: emptyMap()
        val currentMonthPoints = monthlyPointsMap[currentMonth] ?: 0L
        
        // Sprečava negativne poene
        val newTotalPoints = maxOf(0L, currentTotalPoints + pointsChange)
        val newMonthlyPoints = maxOf(0L, currentMonthPoints + pointsChange)
        
        val updatedMonthlyPoints = monthlyPointsMap.toMutableMap()
        updatedMonthlyPoints[currentMonth] = newMonthlyPoints
        
        transaction.update(userRef, mapOf(
            "totalPoints" to newTotalPoints,
            "monthlyPoints" to updatedMonthlyPoints
        ))
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
