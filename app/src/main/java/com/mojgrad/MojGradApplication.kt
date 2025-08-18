package com.mojgrad

import android.app.Application
import com.google.firebase.FirebaseApp

class MojGradApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicijalizuj Firebase
        FirebaseApp.initializeApp(this)
        
        // Za development - osnovni Firebase Auth setup
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            println("DEBUG: Firebase Auth initialized")
        } catch (e: Exception) {
            println("DEBUG: Firebase Auth initialization warning: ${e.message}")
        }
        
        println("DEBUG: MojGrad Firebase initialized successfully")
    }
}
