package com.example.smartcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcare.databinding.ActivityElderlyDashboardBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class ElderlyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElderlyDashboardBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var taskAdapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElderlyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmScheduler = AlarmScheduler(this)
        setSupportActionBar(binding.toolbarElderly)

        setupRecyclerView()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            goToLogin()
            return
        }

        fetchConnectionCode(userId)
        listenForTaskUpdates(userId)

        // Listener untuk menu navigasi bawah
        binding.bottomNavViewElderly.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tasks -> {
                    binding.tasksViewContainer.visibility = View.VISIBLE
                    binding.profileViewContainer.visibility = View.GONE
                    supportActionBar?.title = "Dasbor Tugas Anda"
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

        // Listener untuk tombol logout di dalam tampilan profil
        binding.btnProfileLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadProfileData() {
        binding.tvProfileEmail.text = auth.currentUser?.email ?: "Tidak ditemukan"
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            tasksList, "elderly",
            onItemClick = { task -> handleTaskCompletion(task) },
            onEditClick = {},
            onDeleteClick = {}
        )
        binding.rvTasks.adapter = taskAdapter
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
    }

    private fun handleTaskCompletion(task: Task) {
        val position = tasksList.indexOfFirst { it.id == task.id }
        if (position == -1) return

        if (task.status == "completed" || task.status == "missed") {
            Toast.makeText(this, "Tugas ini sudah selesai atau terlewat.", Toast.LENGTH_SHORT).show()
            taskAdapter.notifyItemChanged(position)
            return
        }

        val reminder = task.reminderTime
        if (reminder == null) {
            updateTaskStatus(task, "completed")
            return
        }

        val currentTime = Date().time
        val reminderTime = reminder.toDate().time
        val deadline = reminderTime + (15 * 60 * 1000)

        if (currentTime < reminderTime) {
            Toast.makeText(this, "Belum waktunya menyelesaikan tugas ini.", Toast.LENGTH_SHORT).show()
            taskAdapter.notifyItemChanged(position)
        } else if (currentTime > deadline) {
            Toast.makeText(this, "Waktu untuk menyelesaikan tugas ini sudah lewat.", Toast.LENGTH_SHORT).show()
            taskAdapter.notifyItemChanged(position)
        } else {
            updateTaskStatus(task, "completed")
        }
    }

    private fun listenForTaskUpdates(userId: String) {
        db.collection("users").document(userId).collection("tasks")
            .orderBy("createdAt")
            .addSnapshotListener { snapshots, e ->
                if (e != null) { return@addSnapshotListener }
                val tasks = snapshots?.toObjects(Task::class.java)?.mapIndexed { index, task ->
                    task.apply { id = snapshots.documents[index].id }
                } ?: emptyList()
                checkForMissedTasks(tasks, userId)
            }
    }

    private fun checkForMissedTasks(tasks: List<Task>, userId: String) {
        val batch = db.batch()
        val currentTime = Date().time
        val tasksToUpdate = mutableListOf<Task>()
        for (task in tasks) {
            val reminder = task.reminderTime
            if (task.status == "pending" && reminder != null) {
                val deadline = reminder.toDate().time + (15 * 60 * 1000)
                if (currentTime > deadline) {
                    val taskRef = db.collection("users").document(userId).collection("tasks").document(task.id)
                    batch.update(taskRef, "status", "missed")
                    tasksToUpdate.add(task)
                }
            }
        }
        if (tasksToUpdate.isNotEmpty()) {
            batch.commit().addOnCompleteListener {
                tasksToUpdate.forEach { alarmScheduler.cancel(it) }
            }
        } else {
            taskAdapter.updateTasks(tasks)
        }
    }

    private fun updateTaskStatus(task: Task, newStatus: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("tasks").document(task.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (newStatus == "completed") {
                    Toast.makeText(this, "Tugas selesai!", Toast.LENGTH_SHORT).show()
                    alarmScheduler.cancel(task)
                }
            }
    }

    private fun fetchConnectionCode(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            binding.tvConnectionCode.text = document.getString("connectionCode") ?: "Kode belum dibuat"
        }
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