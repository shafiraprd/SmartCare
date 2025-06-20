package com.example.smartcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcare.databinding.ActivityElderlyDashboardBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class ElderlyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElderlyDashboardBinding
    private val db = Firebase.firestore
    private lateinit var taskAdapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()
    private lateinit var alarmScheduler: AlarmScheduler

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Aplikasi mungkin tidak dapat menampilkan pengingat tanpa izin notifikasi.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElderlyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmScheduler = AlarmScheduler(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dasbor Lansia"

        setupRecyclerView()

        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            fetchConnectionCode(userId)
            listenForTasks(userId)
        }

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun fetchConnectionCode(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val connectionCode = document.getString("connectionCode")
            if (connectionCode != null) {
                binding.tvConnectionCode.text = connectionCode
            } else {
                val newCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
                db.collection("users").document(userId).update("connectionCode", newCode)
                    .addOnSuccessListener {
                        binding.tvConnectionCode.text = newCode
                    }
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            tasksList,
            "elderly",
            onItemClick = { task ->
                updateTaskStatus(task)
            },
            onEditClick = { /* Tidak ada aksi edit untuk lansia */ },
            onDeleteClick = { /* Tidak ada aksi hapus untuk lansia */ }
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter
    }

    private fun listenForTasks(userId: String) {
        db.collection("users").document(userId).collection("tasks")
            .orderBy("createdAt")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Gagal memuat tugas.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                tasksList.clear()
                for (doc in snapshots!!) {
                    val task = doc.toObject(Task::class.java)
                    task.id = doc.id
                    tasksList.add(task)
                }
                taskAdapter.notifyDataSetChanged()
            }
    }

    private fun updateTaskStatus(task: Task) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        val taskRef = db.collection("users").document(userId).collection("tasks").document(task.id)

        // Cek apakah tugas ini adalah tugas harian dan sedang diselesaikan
        if (task.recurrence == "daily" && task.status == "completed") {
            // Ini adalah tugas harian yang baru saja diselesaikan
            val newReminderTime = task.reminderTime?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it.toDate()
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Tambah 1 hari untuk jadwal besok
                Timestamp(calendar.time)
            }

            // Update tugas: reset status ke "pending" dan atur waktu pengingat baru
            taskRef.update(
                mapOf(
                    "status" to "pending",
                    "reminderTime" to newReminderTime
                )
            ).addOnSuccessListener {
                // Jadwalkan ulang alarm untuk hari berikutnya
                if (newReminderTime != null) {
                    val rescheduledTask = task.copy(reminderTime = newReminderTime)
                    alarmScheduler.schedule(rescheduledTask)
                }
                Toast.makeText(this, "Tugas dijadwalkan ulang untuk besok!", Toast.LENGTH_SHORT).show()
            }

        } else {
            // Ini adalah tugas biasa (tidak berulang) atau tugas yang statusnya diubah kembali ke "pending"
            taskRef.update("status", task.status)
                .addOnSuccessListener {
                    // Jika tugas yang punya alarm dibatalkan (tidak dicentang), batalkan juga alarmnya
                    if (task.status == "pending") {
                        alarmScheduler.cancel(task)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memperbarui status tugas.", Toast.LENGTH_SHORT).show()
                }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                Firebase.auth.signOut()
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}