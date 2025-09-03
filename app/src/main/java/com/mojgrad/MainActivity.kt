package com.mojgrad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mojgrad.manager.ProximityManager
import com.mojgrad.navigation.MojGradNavigation
import com.mojgrad.ui.theme.MojGradTheme

class MainActivity : ComponentActivity() {

    private lateinit var proximityManager: ProximityManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            proximityManager.startProximityMonitoring()
        } else {
            println("DEBUG: MainActivity - Some permissions denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        proximityManager = ProximityManager.getInstance(this)


        checkPermissionsAndStartMonitoring()

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

    }

    private fun checkPermissionsAndStartMonitoring() {
        val requiredPermissions = proximityManager.getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            proximityManager.startProximityMonitoring()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}