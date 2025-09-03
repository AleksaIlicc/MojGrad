package com.mojgrad.util

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ImagePickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    
    fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_${timeStamp}.jpg"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File(storageDir, imageFileName)
        
        return FileProvider.getUriForFile(
            context,
            "com.filips.mojgrad.fileprovider",
            photoFile
        )
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            onImageSelected(cameraImageUri!!)
        }
        onDismiss()
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraImageUri = createImageFile()
                cameraLauncher.launch(cameraImageUri!!)
            } catch (e: Exception) {
                Toast.makeText(context, "Greška pri kreiranju fajla", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        } else {
            Toast.makeText(context, "Dozvola za kameru je potrebna", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
        onDismiss()
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Dodaj fotografiju") },
            text = { Text("Kako želite da dodate fotografiju?") },
            confirmButton = {
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Text("Galerija")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                try {
                                    cameraImageUri = createImageFile()
                                    cameraLauncher.launch(cameraImageUri!!)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Greška pri kreiranju fajla", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                ) {
                    Text("Kamera")
                }
            }
        )
    }
}
