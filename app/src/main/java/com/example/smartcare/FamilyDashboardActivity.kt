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

    private lateinit var binding: ActivityFamilyDashboardBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var elderlyId: String? = null
    private lateinit var elderlyUserAdapter: ElderlyUserAdapter
    private val elderlyUserList = mutableListOf<User>()
    // taskAdapter dan alarmScheduler dipindahkan ke TaskListActivity
    // Namun, kita butuh alarmScheduler di sini untuk deleteTask di masa depan jika diperlukan
    // Untuk saat ini, kita sederhanakan dan hanya fokus pada logika yang ada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupElderlyUserRecyclerView()

        val familyId = auth.currentUser?.uid
        if (familyId == null) {
            goToLogin()
            return
        }
        listenForConnectedElderly(familyId)

        binding.fabAddElderly.setOnClickListener {
            promptToConnect()
        }

        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tasks -> {
                    binding.tasksViewContainer.visibility = View.VISIBLE
                    binding.profileViewContainer.visibility = View.GONE
                    supportActionBar?.title = "Pilih Lansia"
                    true
                }
                R.id.navigation_profile -> {
                    binding.tasksViewContainer.visibility = View.GONE
                    binding.profileViewContainer.visibility = View.VISIBLE
                    supportActionBar?.title = "Profil"
                    loadProfileData()
                    true
                }
                else -> false
            }
        }

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

        binding.btnProfileLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun setupElderlyUserRecyclerView() {
        elderlyUserAdapter = ElderlyUserAdapter(
            elderlyUserList,
            onItemClick = { user ->
                val intent = Intent(this, TaskListActivity::class.java).apply {
                    putExtra("ELDERLY_ID", user.uid)
                    putExtra("ELDERLY_DISPLAY_NAME", if (user.name.isNotEmpty()) user.name else user.email)
                }
                startActivity(intent)
            },
            onDeleteClick = { user ->
                showDeleteConnectionConfirmation(user)
            }
        )
        binding.rvElderlyUsers.adapter = elderlyUserAdapter
        binding.rvElderlyUsers.layoutManager = LinearLayoutManager(this)
    }

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

                for (elderlyId in connectedIds) {
                    db.collection("users").document(elderlyId).get().addOnSuccessListener { elderlyDoc ->
                        val name = elderlyDoc.getString("name") ?: ""
                        val email = elderlyDoc.getString("email") ?: "N/A"
                        val user = User(uid = elderlyId, name = name, email = email, role = "lansia")

                        currentUsers.removeAll { it.uid == user.uid }
                        currentUsers.add(user)

                        if (currentUsers.size == connectedIds.size) {
                            elderlyUserAdapter.updateUsers(currentUsers)
                        }
                    }
                }
            }
    }

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

    private fun deleteElderlyConnection(user: User) {
        val familyId = auth.currentUser?.uid ?: return
        db.collection("users").document(familyId)
            .update("connectedElderlyIds", FieldValue.arrayRemove(user.uid))
            .addOnSuccessListener {
                Toast.makeText(this, "Koneksi dengan ${user.name} telah dihapus.", Toast.LENGTH_SHORT).show()
            }
    }

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

    private fun connectWithElderly(input: String) {
        val isEmail = input.contains("@") && input.contains(".")
        val query = if (isEmail) {
            db.collection("users").whereEqualTo("email", input)
        } else {
            db.collection("users").whereEqualTo("connectionCode", input)
        }
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

    private fun loadProfileData() {
        binding.tvProfileEmail.text = auth.currentUser?.email ?: "Tidak ditemukan"
    }

    private fun logoutUser() {
        auth.signOut()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}