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

    private lateinit var binding: ActivityRegisterBinding // untuk binding layout XML
    private lateinit var auth: FirebaseAuth                // objek FirebaseAuth untuk auth
    private val db = Firebase.firestore                    // objek Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth // inisialisasi Firebase Authentication

        // tombol untuk daftar ditekan
        binding.btnRegister.setOnClickListener { registerUser() }

        // teks "Sudah punya akun? Login" ditekan
        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // tutup activity ini supaya tidak bisa kembali ke sini setelah login
        }
    }

    private fun registerUser() {
        val name = binding.etNameRegister.text.toString().trim()
        val email = binding.etEmailRegister.text.toString().trim()
        val password = binding.etPasswordRegister.text.toString().trim()
        val selectedRoleId = binding.radioGroupRoleRegister.checkedRadioButtonId

        // validasi input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nama, Email, dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        // pastikan peran (role) sudah dipilih
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Silakan pilih peran Anda.", Toast.LENGTH_SHORT).show()
            return
        }

        // ambil peran dari pilihan radio button (keluarga atau lansia)
        val role = findViewById<RadioButton>(selectedRoleId).text.toString().lowercase()

        // mulai proses registrasi akun baru di Firebase
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

                        // jika peran adalah lansia, buat connection code unik
                        if (role == "lansia") {
                            val newCode = (1..6)
                                .map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }
                                .joinToString("")
                            userProfile["connectionCode"] = newCode
                        }

                        // simpan data profil ke Firestore
                        db.collection("users").document(uid).set(userProfile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                                auth.signOut() // logout supaya user login ulang dengan peran
                                startActivity(Intent(this, LoginActivity::class.java))
                                finishAffinity() // hapus semua activity sebelumnya dari stack
                            }
                    }
                } else {
                    // kalau gagal daftar, tampilkan pesan error
                    Toast.makeText(this, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
