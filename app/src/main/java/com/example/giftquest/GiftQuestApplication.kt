package com.example.giftquest

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GiftQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable Firebase Firestore logging for debugging
        FirebaseFirestore.setLoggingEnabled(true)

        // Sign out on app start (remove this line if you want users to stay logged in)
        FirebaseAuth.getInstance().signOut()
    }
}