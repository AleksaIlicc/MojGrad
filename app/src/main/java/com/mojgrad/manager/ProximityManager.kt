package com.mojgrad.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mojgrad.service.ProximityService

class ProximityManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ProximityManager? = null

        fun getInstance(context: Context): ProximityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProximityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var isServiceRunning = false

    fun startProximityMonitoring(): Boolean {
        if (!hasRequiredPermissions()) {
            println("DEBUG: ProximityManager - Missing required permissions")
            return false
        }

        if (!isServiceRunning) {
            ProximityService.startService(context)
            isServiceRunning = true
            println("DEBUG: ProximityManager - Proximity service started")
            return true
        }

        println("DEBUG: ProximityManager - Service already running")
        return true
    }

    fun stopProximityMonitoring() {
        if (isServiceRunning) {
            ProximityService.stopService(context)
            isServiceRunning = false
            println("DEBUG: ProximityManager - Proximity service stopped")
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return locationPermission && notificationPermission
    }

    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    fun isMonitoringActive(): Boolean = isServiceRunning
}
