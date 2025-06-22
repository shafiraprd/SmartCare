package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        // Langkah 1: Otentikasi pengguna dengan email dan password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jika otentikasi berhasil, lanjutkan ke pemeriksaan peran
                    checkUserRole()
                } else {
                    // Jika otentikasi gagal
                    Toast.makeText(this, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return

        // Langkah 2: Ambil data pengguna dari Firestore untuk memeriksa perannya
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")

                    // Langkah 3: Arahkan ke halaman yang sesuai berdasarkan peran
                    when (role) {
                        "keluarga" -> {
                            Toast.makeText(this, "Login sebagai Keluarga berhasil!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, FamilyDashboardActivity::class.java))
                            finishAffinity()
                        }
                        "lansia" -> {
                            Toast.makeText(this, "Login sebagai Lansia berhasil!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ElderlyDashboardActivity::class.java))
                            finishAffinity()
                        }
                        else -> {
                            // Jika peran tidak dikenali
                            Toast.makeText(this, "Peran pengguna tidak dikenali.", Toast.LENGTH_SHORT).show()
                            auth.signOut() // Logout pengguna
                        }
                    }
                } else {
                    // Jika dokumen pengguna tidak ditemukan di Firestore
                    Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener { exception ->
                // Jika gagal mengambil data dari Firestore
                Toast.makeText(this, "Gagal mengambil data peran: ${exception.message}", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    override fun onStart() {
        super.onStart()
        // Cek apakah pengguna sudah login sebelumnya
        if (auth.currentUser != null) {
            // Jika sudah, langsung periksa perannya tanpa perlu login ulang
            checkUserRole()
        }
    }
}