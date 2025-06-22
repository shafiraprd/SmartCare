package com.example.smartcare

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcare.databinding.ActivityFamilyDashboardBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class FamilyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFamilyDashboardBinding // buat akses ke komponen XML via View Binding
    private val db = Firebase.firestore // akses database Firestore
    private val auth = Firebase.auth // akses autentikasi pengguna
    private var elderlyId: String? = null // nanti bisa dipakai untuk simpan ID lansia yang dipilih
    private lateinit var elderlyUserAdapter: ElderlyUserAdapter // adapter untuk daftar lansia
    private val elderlyUserList = mutableListOf<User>() // data user lansia yang terkoneksi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyDashboardBinding.inflate(layoutInflater) // inisialisasi binding
        setContentView(binding.root) // tampilkan layout dari binding

        setSupportActionBar(binding.toolbar) // set toolbar di atas

        setupElderlyUserRecyclerView() // siapkan tampilan daftar lansia

        val familyId = auth.currentUser?.uid // ambil ID user keluarga yang login
        if (familyId == null) {
            goToLogin() // kalau belum login, langsung ke halaman login
            return
        }

        listenForConnectedElderly(familyId) // pantau perubahan data lansia yang terkoneksi

        // tombol untuk menambahkan koneksi ke lansia baru
        binding.fabAddElderly.setOnClickListener {
            promptToConnect()
        }

        // navigasi bawah: toggle antara tampilan tugas dan profil
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tasks -> {
                    // tampilkan tampilan tugas, sembunyikan profil
                    binding.tasksViewContainer.visibility = View.VISIBLE
                    binding.profileViewContainer.visibility = View.GONE
                    supportActionBar?.title = "Pilih Lansia"
                    true
                }
                R.id.navigation_profile -> {
                    // tampilkan tampilan profil, sembunyikan tugas
                    binding.tasksViewContainer.visibility = View.GONE
                    binding.profileViewContainer.visibility = View.VISIBLE
                    supportActionBar?.title = "Profil"
                    loadProfileData() // isi data profil user
                    true
                }
                else -> false
            }
        }

        // tombol ubah password: kirim email reset ke email yang login
        binding.btnChangePassword.setOnClickListener {
            val user = auth.currentUser
            if (user?.email != null) {
                auth.sendPasswordResetEmail(user.email!!)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email untuk reset kata sandi telah dikirim.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // tombol logout
        binding.btnProfileLogout.setOnClickListener {
            logoutUser()
        }
    }

    // Setup tampilan daftar lansia di RecyclerView
    private fun setupElderlyUserRecyclerView() {
        elderlyUserAdapter = ElderlyUserAdapter(
            elderlyUserList,
            onItemClick = { user ->
                // saat item diklik, buka halaman TaskList dan kirim ID serta nama/email user
                val intent = Intent(this, TaskListActivity::class.java).apply {
                    putExtra("ELDERLY_ID", user.uid)
                    putExtra("ELDERLY_DISPLAY_NAME", if (user.name.isNotEmpty()) user.name else user.email)
                }
                startActivity(intent)
            },
            onDeleteClick = { user ->
                showDeleteConnectionConfirmation(user) // tampilkan dialog konfirmasi hapus koneksi
            }
        )
        binding.rvElderlyUsers.adapter = elderlyUserAdapter
        binding.rvElderlyUsers.layoutManager = LinearLayoutManager(this)
    }

    // Dengar terus apakah ada perubahan data lansia yang terhubung
    private fun listenForConnectedElderly(familyId: String) {
        db.collection("users").document(familyId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val connectedIds = snapshot.get("connectedElderlyIds") as? List<String>
                val currentUsers = mutableListOf<User>()

                if (connectedIds.isNullOrEmpty()) {
                    elderlyUserAdapter.updateUsers(emptyList())
                    return@addSnapshotListener
                }

                // ambil data masing-masing lansia yang terhubung
                for (elderlyId in connectedIds) {
                    db.collection("users").document(elderlyId).get().addOnSuccessListener { elderlyDoc ->
                        val name = elderlyDoc.getString("name") ?: ""
                        val email = elderlyDoc.getString("email") ?: "N/A"
                        val user = User(uid = elderlyId, name = name, email = email, role = "lansia")

                        // hindari data duplikat
                        currentUsers.removeAll { it.uid == user.uid }
                        currentUsers.add(user)

                        // update list kalau semua data sudah didapat
                        if (currentUsers.size == connectedIds.size) {
                            elderlyUserAdapter.updateUsers(currentUsers)
                        }
                    }
                }
            }
    }

    // Tampilkan pop-up konfirmasi sebelum menghapus koneksi ke user lansia
    private fun showDeleteConnectionConfirmation(user: User) {
        val displayName = if (user.name.isNotEmpty()) user.name else user.email
        AlertDialog.Builder(this)
            .setTitle("Hapus Koneksi")
            .setMessage("Anda yakin ingin menghapus koneksi dengan $displayName?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("HAPUS") { dialog, _ ->
                deleteElderlyConnection(user)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Proses menghapus ID user lansia dari daftar yang terhubung
    private fun deleteElderlyConnection(user: User) {
        val familyId = auth.currentUser?.uid ?: return
        db.collection("users").document(familyId)
            .update("connectedElderlyIds", FieldValue.arrayRemove(user.uid))
            .addOnSuccessListener {
                Toast.makeText(this, "Koneksi dengan ${user.name} telah dihapus.", Toast.LENGTH_SHORT).show()
            }
    }

    // Tampilkan dialog untuk menghubungkan ke lansia baru
    private fun promptToConnect() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hubungkan dengan Lansia")
        builder.setMessage("Masukkan Email atau Kode Koneksi (User ID) milik pengguna lansia.")
        val input = EditText(this)
        input.hint = "Email atau User ID"
        builder.setView(input)
        builder.setPositiveButton("Hubungkan") { dialog, _ ->
            val inputText = input.text.toString().trim()
            if (inputText.isNotEmpty()) {
                connectWithElderly(inputText)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    // Lakukan pencarian lansia berdasarkan input user (email atau kode koneksi)
    private fun connectWithElderly(input: String) {
        val isEmail = input.contains("@") && input.contains(".")
        val query = if (isEmail) {
            db.collection("users").whereEqualTo("email", input)
        } else {
            db.collection("users").whereEqualTo("connectionCode", input)
        }

        // pastikan user tersebut memang role-nya "lansia"
        query.whereEqualTo("role", "lansia").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Pengguna Lansia tidak ditemukan.", Toast.LENGTH_SHORT).show()
                } else {
                    val foundElderlyId = documents.documents.first().id
                    val familyId = auth.currentUser?.uid!!
                    db.collection("users").document(familyId)
                        .update("connectedElderlyIds", FieldValue.arrayUnion(foundElderlyId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Lansia baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    // Menampilkan email pengguna di halaman profil
    private fun loadProfileData() {
        binding.tvProfileEmail.text = auth.currentUser?.email ?: "Tidak ditemukan"
    }

    // Logout user dari aplikasi
    private fun logoutUser() {
        auth.signOut()
        goToLogin()
    }

    // Navigasi ke halaman login dan hapus semua activity sebelumnya
    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
