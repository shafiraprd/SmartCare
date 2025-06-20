package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Atur Toolbar
        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.title = "Profil & Setelan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Tampilkan tombol kembali

        displayUserInfo()
        setupActionButtons()
    }

    private fun displayUserInfo() {
        val currentUser = auth.currentUser
        binding.tvProfileEmail.text = currentUser?.email ?: "Email tidak ditemukan"
    }

    private fun setupActionButtons() {
        // DIUBAH: Fungsi ganti kata sandi sekarang aktif
        binding.btnChangePassword.setOnClickListener {
            val user = auth.currentUser
            if (user != null && user.email != null) {
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

        binding.btnProfileLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Fungsi untuk handle klik tombol kembali di toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}