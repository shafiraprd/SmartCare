package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityGetStartedBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GetStartedActivity : AppCompatActivity() {

    // View binding untuk akses langsung ke komponen di layout get_started.xml
    private lateinit var binding: ActivityGetStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root) // tampilkan layout dari view binding

        // Cek apakah user sudah login atau belum
        if (Firebase.auth.currentUser != null) {
            // Kalau sudah login, langsung arahkan ke halaman dashboard
            // Untuk sekarang default-nya ke FamilyDashboardActivity
            // Nantinya bisa tambahkan logika peran (role) untuk arahkan ke dashboard yang tepat
            startActivity(Intent(this, FamilyDashboardActivity::class.java))
            finish() // tutup halaman GetStarted supaya tidak bisa balik ke sini pakai tombol back
            return
        }

        // Tombol untuk ke halaman login
        binding.btnGoToSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Tombol untuk ke halaman register (buat akun baru)
        binding.btnGoToSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
