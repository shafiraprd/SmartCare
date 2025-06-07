package com.example.smartcare // Pastikan package ini sesuai dengan proyek Anda

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityLoginBinding // Import ViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // Deklarasikan variabel untuk ViewBinding dan FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup ViewBinding (cara modern untuk mengakses elemen XML)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        auth = Firebase.auth

        // Mengatur apa yang terjadi saat tombol "Login" di-klik
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(baseContext, "Login berhasil.", Toast.LENGTH_SHORT).show()
                            // Di sini kita akan menambahkan navigasi ke halaman dashboard nanti
                            // val intent = Intent(this, MainActivity::class.java)
                            // startActivity(intent)
                            // finish()
                        } else {
                            Toast.makeText(baseContext, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            }
        }

        // Mengatur apa yang terjadi saat tombol "Daftar" di-klik
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(baseContext, "Pendaftaran berhasil. Silakan login.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(baseContext, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}