package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
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
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnLogin.setOnClickListener {
            val selectedRoleId = binding.radioGroupRoleLogin.checkedRadioButtonId
            if (selectedRoleId == -1) {
                Toast.makeText(this, "Silakan pilih peran Anda.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedRole = findViewById<RadioButton>(selectedRoleId).text.toString().lowercase()
            loginUser(selectedRole)
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish() // Selesaikan activity ini agar tidak menumpuk
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
                    // ... (Kode untuk menyimpan FCM token tetap sama) ...
                    val user = task.result?.user
                    if (user != null) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                            if (!tokenTask.isSuccessful) {
                                Log.w("FCM_TOKEN", "Gagal mendapatkan FCM Token.", tokenTask.exception)
                                return@addOnCompleteListener
                            }
                            val token = tokenTask.result
                            val userProfileUpdate = mapOf("fcmToken" to token)
                            db.collection("users").document(user.uid)
                                .set(userProfileUpdate, SetOptions.merge())
                        }
                    }

                    Toast.makeText(baseContext, "Login berhasil!", Toast.LENGTH_SHORT).show()

                    val intent = if (role == "keluarga") {
                        Intent(this, FamilyDashboardActivity::class.java)
                    } else {
                        Intent(this, ElderlyDashboardActivity::class.java)
                    }
                    // Hapus semua activity sebelumnya dari stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(baseContext, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}