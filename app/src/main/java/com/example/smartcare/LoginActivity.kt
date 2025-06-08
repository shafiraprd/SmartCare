// Path: app/src/main/java/com/example/smartcare/LoginActivity.kt

package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

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