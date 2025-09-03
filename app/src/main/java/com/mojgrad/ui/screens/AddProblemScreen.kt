package com.mojgrad.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mojgrad.service.ImageUploadService
import com.mojgrad.ui.viewmodel.AddProblemViewModel
import com.mojgrad.util.ImagePickerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProblemScreen(
    onProblemAdded: () -> Unit,
    viewModel: AddProblemViewModel = viewModel()
) {
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Saobraćaj") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current
    val imageUploadService = remember { ImageUploadService(context) }
    val scope = rememberCoroutineScope()


    val categories = listOf("Saobraćaj", "Čistoća", "Infrastruktura", "Bezbednost", "Ostalo")


    LaunchedEffect(Unit) {
        viewModel.resetState()
    }


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

        Text(
            text = "Prijavi Problem",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )


        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Opis problema") },
            placeholder = { Text("Opišite problem koji ste primetili...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )


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


        Text(
            text = "Dodaj sliku (opciono)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { showImagePicker = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Problem Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )


                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Dodaj sliku",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dodaj sliku problema",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }


        uploadError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Greška pri otpremanju slike: $error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { uploadError = null }) {
                        Text("OK")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                scope.launch {
                    if (selectedImageUri != null) {
                        isUploadingImage = true
                        uploadError = null

                        try {
                            val result = imageUploadService.uploadImage(selectedImageUri!!)
                            result.fold(
                                onSuccess = { uploadResponse ->
                                    viewModel.addProblem(description, selectedCategory, uploadResponse.url)
                                },
                                onFailure = { exception ->
                                    uploadError = exception.message
                                }
                            )
                        } finally {
                            isUploadingImage = false
                        }
                    } else {
                        viewModel.addProblem(description, selectedCategory, null)
                    }
                }
            },
            enabled = description.isNotBlank() &&
                     uploadState != AddProblemViewModel.UploadState.UPLOADING &&
                     !isUploadingImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uploadState == AddProblemViewModel.UploadState.UPLOADING || isUploadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isUploadingImage) "Otprema sliku..." else "Šalje se...")
            } else {
                Text("Pošalji Prijavu")
            }
        }


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


    ImagePickerDialog(
        showDialog = showImagePicker,
        onDismiss = { showImagePicker = false },
        onImageSelected = { uri ->
            selectedImageUri = uri
            uploadError = null
        }
    )
}
