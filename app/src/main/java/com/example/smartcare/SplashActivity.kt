package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2000 // 2 detik
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = Firebase.auth

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, splashTimeOut)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Jika ada pengguna yang sudah login, periksa perannya
            checkUserRoleAndRedirect(currentUser.uid)
        } else {
            // Jika tidak ada yang login, arahkan ke halaman Get Started
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    val intent = when (role) {
                        "keluarga" -> Intent(this, FamilyDashboardActivity::class.java)
                        "lansia" -> Intent(this, ElderlyDashboardActivity::class.java)
                        else -> null // Peran tidak diketahui
                    }

                    if (intent != null) {
                        startActivity(intent)
                        finish()
                    } else {
                        // Jika peran tidak ada atau tidak valid, logout dan ke halaman login
                        Toast.makeText(this, "Peran pengguna tidak dikenali.", Toast.LENGTH_SHORT).show()
                        logoutAndGoToLogin()
                    }
                } else {
                    // Jika data tidak ada di firestore (kasus anomali)
                    Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    logoutAndGoToLogin()
                }
            }
            .addOnFailureListener {
                // Jika gagal mengambil data dari firestore
                Toast.makeText(this, "Gagal memverifikasi sesi.", Toast.LENGTH_SHORT).show()
                logoutAndGoToLogin()
            }
    }

    private fun logoutAndGoToLogin() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}