package com.mojgrad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mojgrad.ui.theme.MojGradTheme
import com.mojgrad.ui.viewmodel.AuthUiState
import com.mojgrad.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AuthUiState,
    onLogoutClick: () -> Unit,
    onMapClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Proverava da li je korisnik učitan
        if (uiState.isLoading) {
            CircularProgressIndicator()
            Text(
                text = "Učitavam podatke...",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            uiState.currentUser?.let { user ->
                // Default avatar (uvek prikaži)
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pozdravna poruka sa imenom
                Text(
                    text = "Dobrodošli, ${user.name}!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Email
                Text(
                    text = user.email,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                // Broj telefona
                if (user.phoneNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.phoneNumber,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                
                // User ID za debug
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "UID: ${user.uid.take(8)}...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
            } ?: run {
                // Fallback ako nema korisničkih podataka
                Text(
                    text = "Dobrodošli u MojGrad!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
                
                Text(
                    text = "Učitavam vaše podatke...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Error prikaz
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Greška: $error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Dugme za pristup mapi
        Button(
            onClick = onMapClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Prikaži Mapu")
        }

        // Dugme za odjavu (privremeno)
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Odjavi se")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MojGradTheme {
        HomeScreen(
            uiState = AuthUiState(
                isLoggedIn = true,
                currentUser = User(
                    uid = "123",
                    name = "Marko Marković",
                    email = "marko@example.com",
                    phoneNumber = "+381 60 123 4567"
                )
            ),
            onLogoutClick = { }
        )
    }
}
