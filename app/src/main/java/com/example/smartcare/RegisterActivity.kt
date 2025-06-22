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
        binding.btnRegister.setOnClickListener { registerUser() }
        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etNameRegister.text.toString().trim()
        val email = binding.etEmailRegister.text.toString().trim()
        val password = binding.etPasswordRegister.text.toString().trim()
        val selectedRoleId = binding.radioGroupRoleRegister.checkedRadioButtonId

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nama, Email, dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Silakan pilih peran Anda.", Toast.LENGTH_SHORT).show()
            return
        }
        val role = findViewById<RadioButton>(selectedRoleId).text.toString().lowercase()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userProfile = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role
                        )
                        // Buat kode koneksi unik HANYA jika perannya lansia
                        if (role == "lansia") {
                            val newCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
                            userProfile["connectionCode"] = newCode
                        }

                        db.collection("users").document(uid).set(userProfile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                                auth.signOut() // Langsung logout agar user harus login manual
                                startActivity(Intent(this, LoginActivity::class.java))
                                finishAffinity()
                            }
                    }
                } else {
                    Toast.makeText(this, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}