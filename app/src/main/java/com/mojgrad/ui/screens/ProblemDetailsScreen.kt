package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mojgrad.ui.viewmodel.ProblemDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailsScreen(
    problemId: String,
    onBackClick: () -> Unit,
    viewModel: ProblemDetailsViewModel = viewModel()
) {
    val problem by viewModel.problem.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val voteState by viewModel.voteState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasUserVoted by viewModel.hasUserVoted.collectAsState()

    // Učitaj problem kada se ekran otvori
    LaunchedEffect(problemId) {
        viewModel.loadProblem(problemId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Detalji Problema") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            problem?.let { currentProblem ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Kategorija
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = currentProblem.category,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Opis
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Opis problema",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentProblem.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Mapa
                    currentProblem.location?.let { geoPoint ->
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Lokacija",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(
                                        LatLng(geoPoint.latitude, geoPoint.longitude), 15f
                                    )
                                }
                                
                                GoogleMap(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(
                                        isMyLocationEnabled = false
                                    )
                                ) {
                                    Marker(
                                        state = MarkerState(
                                            position = LatLng(geoPoint.latitude, geoPoint.longitude)
                                        ),
                                        title = currentProblem.category,
                                        snippet = currentProblem.description
                                    )
                                }
                            }
                        }
                    }

                    // Glasanje sekcija
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Glasova: ${currentProblem.votes}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Potvrdi važnost ovog problema",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.voteForProblem(currentProblem.id, currentProblem.userId)
                                },
                                enabled = !hasUserVoted && voteState != ProblemDetailsViewModel.VoteState.VOTING,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (voteState == ProblemDetailsViewModel.VoteState.VOTING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Glasam...")
                                } else if (hasUserVoted) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Već ste glasali ✓")
                                } else {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Potvrdi Problem (+1 poen)")
                                }
                            }
                        }
                    }

                    // Status
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Status: ${when(currentProblem.status) {
                                    "pending" -> "Na čekanju"
                                    "in_progress" -> "U toku"
                                    "resolved" -> "Rešeno"
                                    else -> "Nepoznato"
                                }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Error handling
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Ovde možeš da dodaš SnackBar ili Toast
                println("DEBUG: Error prikazan: $error")
            }
        }

        // Success handling
        if (voteState == ProblemDetailsViewModel.VoteState.SUCCESS) {
            LaunchedEffect(voteState) {
                println("DEBUG: Uspešno glasanje!")
                viewModel.clearError()
            }
        }
        
        if (voteState == ProblemDetailsViewModel.VoteState.ALREADY_VOTED) {
            LaunchedEffect(voteState) {
                println("DEBUG: Već je glasao")
                viewModel.clearError()
            }
        }
    }
}
