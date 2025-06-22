package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding // untuk akses langsung ke komponen layout
    private val auth = Firebase.auth // inisialisasi Firebase Authentication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // atur toolbar di bagian atas halaman profil
        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.title = "Profil & Setelan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // aktifkan tombol panah kembali di toolbar

        displayUserInfo()       // tampilkan informasi user (email)
        setupActionButtons()    // atur aksi pada tombol-tombol
    }

    private fun displayUserInfo() {
        // ambil user yang sedang login, lalu tampilkan email-nya
        val currentUser = auth.currentUser
        binding.tvProfileEmail.text = currentUser?.email ?: "Email tidak ditemukan"
    }

    private fun setupActionButtons() {
        // tombol ganti password ditekan
        binding.btnChangePassword.setOnClickListener {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // kirim email reset password ke alamat email user
                auth.sendPasswordResetEmail(user.email!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email untuk reset kata sandi telah dikirim.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Gagal mengirim email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Tidak dapat menemukan email pengguna untuk mengirim link reset.", Toast.LENGTH_LONG).show()
            }
        }

        // tombol logout ditekan
        binding.btnProfileLogout.setOnClickListener {
            auth.signOut() // logout dari akun Firebase
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

            // arahkan kembali ke halaman login, dan hapus semua activity sebelumnya
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // akhiri activity ini
        }
    }

    // fungsi ini dipanggil saat tombol back (panah) di toolbar ditekan
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // kembali ke halaman sebelumnya
        return true
    }
}
