package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore // Tambahkan ini untuk akses ke Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // Jika user sudah login dari sesi sebelumnya, langsung arahkan
        if (auth.currentUser != null) {
            // Kita default ke FamilyDashboard untuk sementara
            val intent = Intent(this, FamilyDashboardActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Mengatur listener untuk tombol login baru
        binding.btnLoginKeluarga.setOnClickListener {
            loginUser("keluarga")
        }

        binding.btnLoginLansia.setOnClickListener {
            loginUser("lansia")
        }

        // Mengatur listener untuk tombol daftar
        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, "Pendaftaran berhasil. Silakan login.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginUser(role: String) {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    // ▼▼▼ KODE BARU DIMASUKKAN DI SINI ▼▼▼
                    // Setelah login berhasil, dapatkan dan simpan FCM Token
                    val user = task.result?.user
                    if (user != null) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                            if (!tokenTask.isSuccessful) {
                                Log.w("FCM_TOKEN", "Gagal mendapatkan FCM Token.", tokenTask.exception)
                                return@addOnCompleteListener
                            }
                            val token = tokenTask.result
                            // Buat data untuk di-update di profil user
                            val userProfileUpdate = mapOf("fcmToken" to token)

                            // Simpan token ke dokumen user di koleksi "users"
                            db.collection("users").document(user.uid)
                                .set(userProfileUpdate, SetOptions.merge())
                                .addOnSuccessListener { Log.d("FCM_TOKEN", "FCM Token berhasil disimpan ke Firestore.") }
                                .addOnFailureListener { e -> Log.w("FCM_TOKEN", "Gagal menyimpan FCM Token.", e) }
                        }
                    }
                    // ▲▲▲ AKHIR DARI KODE BARU ▲▲▲

                    Toast.makeText(baseContext, "Login berhasil!", Toast.LENGTH_SHORT).show()

                    // Tentukan tujuan berdasarkan peran
                    val intent = if (role == "keluarga") {
                        Intent(this, FamilyDashboardActivity::class.java)
                    } else {
                        Intent(this, ElderlyDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(baseContext, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}