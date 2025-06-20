package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000 // Durasi splash screen dalam milidetik (2 detik)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Cek apakah pengguna sudah login sebelumnya
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                // Jika sudah login, arahkan langsung ke dasbor.
                // Idealnya, kita harus memeriksa peran pengguna (Keluarga/Lansia) dari Firestore.
                // Untuk saat ini, kita arahkan ke FamilyDashboardActivity sebagai default.
                startActivity(Intent(this, FamilyDashboardActivity::class.java))
            } else {
                // Jika belum login, arahkan ke halaman Get Started
                startActivity(Intent(this, GetStartedActivity::class.java))
            }

            // Tutup SplashActivity agar pengguna tidak bisa kembali ke sini dengan tombol "back"
            finish()
        }, SPLASH_TIME_OUT)
    }
}