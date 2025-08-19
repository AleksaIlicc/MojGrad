package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mojgrad.data.model.Problem
import com.mojgrad.ui.viewmodel.ListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onMapClick: (Problem) -> Unit = {},
    viewModel: ListViewModel = viewModel()
) {
    val problems by viewModel.problems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userVotes by viewModel.userVotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text("Problemi")
            },
            actions = {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Filteri")
                }
            }
        )
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Pretraži probleme...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Obriši")
                    }
                }
            },
            singleLine = true
        )
        
        // Error message kao snackbar na vrhu
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = "×",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Učitavam probleme...")
                }
            }
        } else if (problems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Nema rezultata pretrage" else "Nema aktivnih problema",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Pokušajte sa drugim pojmom" else "Prijavite prvi problem na mapi!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(problems) { problem ->
                    ProblemListItem(
                        problem = problem,
                        hasVoted = userVotes[problem.id] == true,
                        onVoteClick = { viewModel.toggleVoteForProblem(problem) },
                        onMapClick = { onMapClick(problem) }
                    )
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun ProblemListItem(
    problem: Problem,
    hasVoted: Boolean,
    onVoteClick: () -> Unit,
    onMapClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header sa imenom autora (levo) i kategorijom (desno)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Ime autora
                Text(
                    text = problem.authorName.ifEmpty { "Nepoznat korisnik" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                // Kategorija
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = problem.category.ifEmpty { "Ostalo" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timestamp
            if (problem.timestamp != null) {
                val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text(
                    text = formatter.format(problem.timestamp!!),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Opis problema
            Text(
                text = problem.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer sa glasovima i akcijama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vote dugme
                OutlinedButton(
                    onClick = onVoteClick,
                    modifier = Modifier.height(36.dp),
                    colors = if (hasVoted) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (hasVoted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${problem.votes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Map dugme
                if (problem.location != null) {
                    OutlinedButton(
                        onClick = onMapClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lokacija",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    viewModel: ListViewModel,
    onDismiss: () -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedAuthor by viewModel.selectedAuthor.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    
    // Local state for temporary changes
    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var tempAuthor by remember { mutableStateOf(selectedAuthor) }
    var tempStatus by remember { mutableStateOf(selectedStatus) }
    var tempSortBy by remember { mutableStateOf(sortBy) }
    var tempStartDate by remember { mutableStateOf(dateRange.first) }
    var tempEndDate by remember { mutableStateOf(dateRange.second) }
    
    // Dropdown states
    var categoryExpanded by remember { mutableStateOf(false) }
    var authorExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    
    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Get options
    val categories = remember(viewModel) { listOf("Sve kategorije") + viewModel.getUniqueCategories() }
    val authors = remember(viewModel) { listOf("Svi autori") + viewModel.getUniqueAuthors() }
    val statuses = listOf("PRIJAVLJENO", "REŠENO")
    val sortOptions = listOf("newest" to "Najnoviji", "votes" to "Po glasovima")
    
    // Date formatters
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filteri i sortiranje")
                TextButton(
                    onClick = {
                        tempCategory = null
                        tempAuthor = null
                        tempStatus = "PRIJAVLJENO"
                        tempSortBy = "newest"
                        tempStartDate = null
                        tempEndDate = null
                    }
                ) {
                    Text("Poništi sve", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Filter
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = tempCategory ?: "Sve kategorije",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Kategorija") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    tempCategory = if (category == "Sve kategorije") null else category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Author Filter
                ExposedDropdownMenuBox(
                    expanded = authorExpanded,
                    onExpandedChange = { authorExpanded = !authorExpanded }
                ) {
                    OutlinedTextField(
                        value = tempAuthor ?: "Svi autori",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Autor") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = authorExpanded,
                        onDismissRequest = { authorExpanded = false }
                    ) {
                        authors.forEach { author ->
                            DropdownMenuItem(
                                text = { Text(author) },
                                onClick = {
                                    tempAuthor = if (author == "Svi autori") null else author
                                    authorExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Status Filter
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = tempStatus,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    tempStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Sort By
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }
                ) {
                    OutlinedTextField(
                        value = sortOptions.find { it.first == tempSortBy }?.second ?: "Najnoviji",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Sortiranje") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        sortOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    tempSortBy = key
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Date Range Section
                Text(
                    text = "Opseg datuma",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                // Start Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempStartDate?.let { 
                            "${dateFormatter.format(Date(it))} ${timeFormatter.format(Date(it))}"
                        } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Od datuma i vremena") },
                        placeholder = { Text("Izaberite datum i vreme") },
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (tempStartDate != null) {
                        IconButton(
                            onClick = { tempStartDate = null },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Obriši")
                        }
                    }
                }
                
                // End Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempEndDate?.let { 
                            "${dateFormatter.format(Date(it))} ${timeFormatter.format(Date(it))}"
                        } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Do datuma i vremena") },
                        placeholder = { Text("Izaberite datum i vreme") },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (tempEndDate != null) {
                        IconButton(
                            onClick = { tempEndDate = null },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Obriši")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Apply filters
                    viewModel.updateCategoryFilter(tempCategory)
                    viewModel.updateAuthorFilter(tempAuthor)
                    viewModel.updateStatusFilter(tempStatus)
                    viewModel.updateSortBy(tempSortBy)
                    viewModel.updateDateRange(tempStartDate, tempEndDate)
                    onDismiss()
                }
            ) {
                Text("Primeni")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Otkaži")
            }
        }
    )
    
    // Date Pickers
    if (showStartDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { selectedDateTimeMillis ->
                tempStartDate = selectedDateTimeMillis
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { selectedDateTimeMillis ->
                tempEndDate = selectedDateTimeMillis
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(0) }
    var selectedMinute by remember { mutableStateOf(0) }
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text("Sledeće")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Otkaži")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showTimePicker && selectedDate != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Izaberite vreme") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Combine date and time
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate!!
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateTimeSelected(calendar.timeInMillis)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        showDatePicker = true
                    }
                ) {
                    Text("Nazad")
                }
            }
        )
    }
}
