package com.mojgrad.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.mojgrad.R
import com.mojgrad.data.model.Problem
import com.mojgrad.location.LocationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ProximityService : Service() {
    
    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val PROXIMITY_NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "ProximityServiceChannel"
        const val PROXIMITY_CHANNEL_ID = "ProximityNotificationChannel"
        const val DEFAULT_RADIUS_KM = 0.5 // 500 metara default
        
        fun startService(context: Context) {
            val intent = Intent(context, ProximityService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, ProximityService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var locationManager: LocationManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationManager: NotificationManagerCompat
    
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var problemsListener: ListenerRegistration? = null
    private var lastNotifiedProblems = mutableSetOf<String>()
    
    private val proximityRadiusKm = DEFAULT_RADIUS_KM
    
    override fun onCreate() {
        super.onCreate()
        
        println("DEBUG: ProximityService - Service created, initializing...")
        
        locationManager = LocationManager.getInstance(this)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        notificationManager = NotificationManagerCompat.from(this)
        
        createNotificationChannels()
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
        
        println("DEBUG: ProximityService - Service initialized, radius: ${proximityRadiusKm}km")
        startProximityMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        problemsListener?.remove()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Kanal za foreground service
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Prati vašu lokaciju za proximity obaveštenja"
                setShowBadge(false)
            }
            
            // Kanal za proximity notifikacije
            val proximityChannel = NotificationChannel(
                PROXIMITY_CHANNEL_ID,
                "Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obaveštenja kada ste blizu prijavljenih problema"
                enableVibration(true)
                setShowBadge(true)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(proximityChannel)
        }
    }
    
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MojGrad - Background Monitoring")
            .setContentText("Praćenje blizine prijavljenih problema")
            .setSmallIcon(R.drawable.ic_location_24)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun startProximityMonitoring() {
        println("DEBUG: ProximityService - Starting proximity monitoring...")
        println("DEBUG: ProximityService - LocationManager instance: $locationManager")
        serviceScope.launch {
            // Umesto praćenja location flow-a, koristi timer da proverava lokaciju svakih 30 sekundi
            while (true) {
                val currentLocation = locationManager.currentLocation.value
                if (currentLocation != null) {
                    println("DEBUG: ProximityService - Timer check - Location available, checking proximity...")
                    checkProximityToProblems(currentLocation)
                } else {
                    println("DEBUG: ProximityService - Timer check - No location available")
                }
                
                // Čekaj 30 sekundi pre sledećeg check-a
                delay(30000)
            }
        }
        
        // Dodaj heartbeat da vidim da li service još uvek radi
        serviceScope.launch {
            while (true) {
                delay(30000) // svakih 30 sekundi
                println("DEBUG: ProximityService - Service heartbeat - still running. Problems in cooldown: ${lastNotifiedProblems.size}")
                lastNotifiedProblems.forEach { problemId ->
                    println("DEBUG: ProximityService - Problem in cooldown: $problemId")
                }
            }
        }
    }
    
    private suspend fun checkProximityToProblems(userLocation: LatLng) {
        val currentUser = auth.currentUser ?: return
        
        println("DEBUG: ProximityService - Checking proximity for user: ${currentUser.uid}")
        println("DEBUG: ProximityService - User location: ${userLocation.latitude}, ${userLocation.longitude}")
        
        try {
            // Dohvati sve aktivne probleme
            val problemsSnapshot = firestore.collection("problems")
                .whereEqualTo("status", "PRIJAVLJENO")
                .get()
                .await()
            
            println("DEBUG: ProximityService - Found ${problemsSnapshot.documents.size} active problems")
            
            val userGeoLocation = GeoLocation(userLocation.latitude, userLocation.longitude)
            var candidateProblems = 0
            var notificationsSent = 0
            var skippedOwnProblems = 0
            var skippedTooFar = 0
            var skippedAlreadyNotified = 0
            
            problemsSnapshot.documents.forEach { document ->
                try {
                    val problem = document.toObject(Problem::class.java)?.copy(id = document.id)
                    if (problem != null) {
                        candidateProblems++
                        
                        if (problem.userId == currentUser.uid) {
                            skippedOwnProblems++
                            println("DEBUG: ProximityService - Skipped own problem: ${problem.id}")
                            return@forEach
                        }
                        
                        val problemGeoLocation = GeoLocation(
                            problem.location!!.latitude,
                            problem.location.longitude
                        )
                        
                        val distanceMeters = GeoFireUtils.getDistanceBetween(
                            userGeoLocation, problemGeoLocation
                        )
                        val distanceKm = distanceMeters / 1000.0
                        
                        println("DEBUG: ProximityService - Problem ${problem.id} distance: ${distanceKm}km (threshold: ${proximityRadiusKm}km)")
                        
                        if (distanceKm <= proximityRadiusKm) {
                            // Korisnik je blizu ovog problema
                            if (!lastNotifiedProblems.contains(problem.id)) {
                                showProximityNotification(problem, distanceKm)
                                lastNotifiedProblems.add(problem.id)
                                notificationsSent++
                                println("DEBUG: ProximityService - NOTIFICATION SENT for problem: ${problem.id}")
                                
                                // Ukloni iz liste nakon 2 minuta da može ponovo da obavesti
                                serviceScope.launch {
                                    delay(2 * 60 * 1000) // 2 minuta
                                    lastNotifiedProblems.remove(problem.id)
                                    println("DEBUG: ProximityService - Removed ${problem.id} from cooldown list after 2 minutes")
                                }
                            } else {
                                skippedAlreadyNotified++
                                println("DEBUG: ProximityService - Skipped already notified problem: ${problem.id}")
                            }
                        } else {
                            skippedTooFar++
                            println("DEBUG: ProximityService - Problem ${problem.id} too far: ${distanceKm}km")
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Error processing problem: ${e.message}")
                }
            }
            
            println("DEBUG: ProximityService - SUMMARY:")
            println("DEBUG: ProximityService - Total problems checked: $candidateProblems")
            println("DEBUG: ProximityService - Own problems skipped: $skippedOwnProblems")
            println("DEBUG: ProximityService - Too far problems: $skippedTooFar")
            println("DEBUG: ProximityService - Already notified (cooldown): $skippedAlreadyNotified")
            println("DEBUG: ProximityService - NOTIFICATIONS SENT: $notificationsSent")
            println("DEBUG: ProximityService - Currently in cooldown: ${lastNotifiedProblems.size} problems")
            
        } catch (e: Exception) {
            println("DEBUG: Error checking proximity: ${e.message}")
        }
    }
    
    private fun showProximityNotification(problem: Problem, distanceKm: Double) {
        // Kreiranje intent-a za otvaranje aplikacije sa problemom
        val intent = Intent(this, com.mojgrad.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("problemId", problem.id)
            putExtra("openProblemDetail", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            problem.id.hashCode(),
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val distanceText = if (distanceKm < 0.1) {
            "manje od 100m"
        } else {
            "${String.format("%.0f", distanceKm * 1000)}m"
        }
        
        val notification = NotificationCompat.Builder(this, PROXIMITY_CHANNEL_ID)
            .setContentTitle("Problem u blizini!")
            .setContentText("${problem.category} na $distanceText od vas")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${problem.category}: ${problem.description}\n\nUdaljenost: $distanceText"))
            .setSmallIcon(R.drawable.ic_location_24)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(
            PROXIMITY_NOTIFICATION_ID + problem.id.hashCode(), 
            notification
        )
        
        println("DEBUG: Proximity notification shown for problem: ${problem.id}, distance: ${distanceKm}km")
    }
}
