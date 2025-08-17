package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mojgrad.ui.viewmodel.AddProblemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProblemScreen(
    onProblemAdded: () -> Unit,
    viewModel: AddProblemViewModel = viewModel()
) {
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Saobraćaj") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val uploadState by viewModel.uploadState.collectAsState()
    
    // Kategorije problema
    val categories = listOf("Saobraćaj", "Čistoća", "Infrastruktura", "Bezbednost", "Ostalo")

    // Reagovanje na uspešan upload
    LaunchedEffect(uploadState) {
        if (uploadState == AddProblemViewModel.UploadState.SUCCESS) {
            onProblemAdded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Prijavi Problem",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Opis problema
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Opis problema") },
            placeholder = { Text("Opišite problem koji ste primetili...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Dropdown za kategoriju
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { },
                readOnly = true,
                label = { Text("Kategorija") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dugme za slanje
        Button(
            onClick = {
                viewModel.addProblem(description, selectedCategory)
            },
            enabled = description.isNotBlank() && uploadState != AddProblemViewModel.UploadState.UPLOADING,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uploadState == AddProblemViewModel.UploadState.UPLOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Šalje se...")
            } else {
                Text("Pošalji Prijavu")
            }
        }

        // Prikaz greške
        if (uploadState == AddProblemViewModel.UploadState.ERROR) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Došlo je do greške pri slanju prijave. Pokušajte ponovo.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
