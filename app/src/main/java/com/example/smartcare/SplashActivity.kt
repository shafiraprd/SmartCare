package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2000 // durasi splash screen: 2 detik
    private lateinit var auth: FirebaseAuth // auth untuk cek login
    private val db = Firebase.firestore // firestore database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = Firebase.auth

        // jalankan fungsi checkUserSession setelah 2 detik (splash selesai)
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, splashTimeOut)
    }

    // cek apakah user sudah login atau belum
    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // jika sudah login, periksa role-nya dari database
            checkUserRoleAndRedirect(currentUser.uid)
        } else {
            // kalau belum login, langsung ke halaman GetStarted
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        }
    }

    // ambil data pengguna berdasarkan UID dan arahkan sesuai perannya
    private fun checkUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")

                    // arahkan user ke halaman yang sesuai dengan perannya
                    val intent = when (role) {
                        "keluarga" -> Intent(this, FamilyDashboardActivity::class.java)
                        "lansia" -> Intent(this, ElderlyDashboardActivity::class.java)
                        else -> null // peran tidak valid
                    }

                    if (intent != null) {
                        startActivity(intent)
                        finish()
                    } else {
                        // role tidak dikenali → logout dan ke halaman login
                        Toast.makeText(this, "Peran pengguna tidak dikenali.", Toast.LENGTH_SHORT).show()
                        logoutAndGoToLogin()
                    }
                } else {
                    // data user tidak ditemukan di Firestore (mungkin akun baru tapi belum lengkap)
                    Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    logoutAndGoToLogin()
                }
            }
            .addOnFailureListener {
                // jika terjadi error saat ambil data user dari Firestore
                Toast.makeText(this, "Gagal memverifikasi sesi.", Toast.LENGTH_SHORT).show()
                logoutAndGoToLogin()
            }
    }

    // fungsi untuk logout user dan pindah ke halaman login
    private fun logoutAndGoToLogin() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity() // hapus semua activity sebelumnya dari backstack
    }
}
