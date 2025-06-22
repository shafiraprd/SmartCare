package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcare.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // view binding untuk akses layout
    private lateinit var auth: FirebaseAuth // objek autentikasi Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // inisialisasi binding
        setContentView(binding.root) // set tampilan dari file layout XML

        auth = Firebase.auth // ambil instance Firebase Auth

        val currentUser = auth.currentUser // cek apakah user sudah login

        if (currentUser == null) {
            // kalau belum login, langsung arahkan ke halaman login
            goToLoginActivity()
        } else {
            // kalau sudah login, tampilkan pesan sambutan dengan email user
            binding.tvWelcomeMessage.text = "Selamat Datang, \n${currentUser.email}"
        }

        // ketika tombol logout ditekan
        binding.btnLogout.setOnClickListener {
            auth.signOut() // keluar dari akun
            goToLoginActivity() // kembali ke halaman login
        }
    }

    // fungsi bantu untuk berpindah ke LoginActivity dan menutup activity ini
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // supaya user nggak bisa kembali ke MainActivity setelah logout
    }
}
