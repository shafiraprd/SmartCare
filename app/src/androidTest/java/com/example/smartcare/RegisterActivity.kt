package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Selesaikan activity ini
        }
    }

    private fun registerUser() {
        val email = binding.etEmailRegister.text.toString().trim()
        val password = binding.etPasswordRegister.text.toString().trim()
        val selectedRoleId = binding.radioGroupRoleRegister.checkedRadioButtonId

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Silakan pilih peran Anda (Keluarga/Lansia).", Toast.LENGTH_SHORT).show()
            return
        }

        val role = findViewById<RadioButton>(selectedRoleId).text.toString().lowercase()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid

                    if (uid != null) {
                        // Simpan peran pengguna di Firestore
                        val userProfile = mapOf("role" to role)
                        db.collection("users").document(uid)
                            .set(userProfile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                                // Pindah ke halaman login setelah daftar
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finishAffinity() // Hapus semua activity sebelumnya
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data peran: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}