package com.mojgrad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.mojgrad.navigation.MojGradNavigation
import com.mojgrad.ui.theme.MojGradTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MojGradTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MojGradNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}