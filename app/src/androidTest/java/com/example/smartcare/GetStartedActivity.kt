package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityGetStartedBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GetStartedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Jika user sudah login, langsung ke dashboard yang sesuai
        // (Kita akan tambahkan logika pengecekan peran nanti, untuk sekarang ke Family)
        if (Firebase.auth.currentUser != null) {
            // Untuk sementara, kita arahkan ke FamilyDashboardActivity
            // Idealnya, Anda harus menyimpan peran user dan mengarahkannya dengan benar
            startActivity(Intent(this, FamilyDashboardActivity::class.java))
            finish()
            return
        }

        binding.btnGoToSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnGoToSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}