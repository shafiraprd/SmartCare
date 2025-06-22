package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
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
        val selectedRoleId = binding.radioGroupRoleLogin.checkedRadioButtonId

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Silakan pilih peran login Anda (Keluarga/Lansia).", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil peran yang dipilih pengguna di layar
        val selectedRole = findViewById<RadioButton>(selectedRoleId).text.toString().lowercase()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jika otentikasi berhasil, lanjutkan ke validasi peran
                    validateUserRole(selectedRole)
                } else {
                    Toast.makeText(this, "Login Gagal: Email atau Password Salah.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateUserRole(selectedRole: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val databaseRole = document.getString("role")

                    // --- INTI LOGIKA BARU ADA DI SINI ---
                    if (databaseRole == selectedRole) {
                        // Jika peran yang dipilih di layar SAMA DENGAN peran di database
                        val intent = when (databaseRole) {
                            "keluarga" -> Intent(this, FamilyDashboardActivity::class.java)
                            "lansia" -> Intent(this, ElderlyDashboardActivity::class.java)
                            else -> null
                        }
                        if (intent != null) {
                            Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                            startActivity(intent)
                            finishAffinity()
                        } else {
                            Toast.makeText(this, "Peran pengguna tidak valid.", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                    } else {
                        // Jika peran yang dipilih TIDAK COCOK dengan yang ada di database
                        Toast.makeText(this, "Login gagal. Akun ini terdaftar sebagai '$databaseRole', bukan '$selectedRole'.", Toast.LENGTH_LONG).show()
                        auth.signOut() // Logout paksa karena peran tidak cocok
                    }
                } else {
                    Toast.makeText(this, "Data profil pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memverifikasi peran pengguna.", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    // Fungsi onStart tidak perlu diubah, karena ia akan memanggil validateUserRole
    // yang akan menangani pengalihan ke dasbor yang benar secara otomatis tanpa
    // input dari radio button.
}