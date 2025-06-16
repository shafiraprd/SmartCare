package com.example.smartcare

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcare.databinding.ActivityElderlyDashboardBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import java.util.Date

class ElderlyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElderlyDashboardBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var alarmManager: AlarmManager

    // Handler dan Runnable untuk refresh otomatis
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var refreshRunnable: Runnable
    private val REFRESH_INTERVAL_MS = 60000L // Refresh setiap 60 detik (1 menit)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Anda tidak akan menerima pengingat jadwal.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElderlyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvUserCode.text = currentUser.uid.take(8).uppercase()
            createProfileForLansiaIfNeeded()
            askNotificationPermission()
        } else {
            goToLoginActivity()
            return
        }

        setupRecyclerView()
        listenForTaskUpdates()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLoginActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        // Mulai refresh otomatis saat halaman aktif
        setupAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        // Hentikan refresh otomatis saat halaman tidak aktif untuk hemat baterai
        handler.removeCallbacks(refreshRunnable)
    }

    private fun setupAutoRefresh() {
        refreshRunnable = Runnable {
            Log.d("AutoRefreshChecker", "Handler berjalan, me-refresh UI pada: ${Date()}")
            // Memberitahu adapter untuk menggambar ulang semua item yang terlihat
            if (::taskAdapter.isInitialized) {
                taskAdapter.notifyDataSetChanged()
            }
            // Jadwalkan refresh berikutnya
            handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
        }
        // Langsung jalankan untuk pertama kali
        handler.post(refreshRunnable)
    }

    private fun listenForTaskUpdates() {
        val currentUser = auth.currentUser ?: return
        db.collection("tasks")
            .whereEqualTo("assignedTo", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ElderlyDashboard", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val tasks = snapshots.toObjects<Task>()
                    taskAdapter.updateTasks(tasks)
                    scheduleAlarmsForTasks(tasks)
                }
            }
    }

    private fun scheduleAlarmsForTasks(tasks: List<Task>) {
        for (task in tasks) {
            if (!task.isCompleted && task.reminderTime != null && task.reminderTime!!.toDate().after(Date())) {
                scheduleAlarm(task)
            }
        }
    }

    private fun scheduleAlarm(task: Task) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_ID", task.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, task.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime!!.toDate().time, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime!!.toDate().time, pendingIntent)
        }
    }

    private fun cancelAlarm(task: Task) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, task.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun markTaskAsCompleted(task: Task) {
        db.collection("tasks").document(task.id)
            .update("isCompleted", true)
            .addOnSuccessListener {
                Toast.makeText(this, "'${task.title}' telah selesai!", Toast.LENGTH_SHORT).show()
                cancelAlarm(task)
            }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            mutableListOf(),
            "lansia",
            onItemClick = { task -> markTaskAsCompleted(task) },
            onEditClick = {},
            onDeleteClick = {}
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@ElderlyDashboardActivity)
            adapter = taskAdapter
        }
    }

    private fun createProfileForLansiaIfNeeded() {
        val currentUser = auth.currentUser ?: return
        val userDocRef = db.collection("users").document(currentUser.uid)
        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userProfile = mapOf("role" to "lansia")
                userDocRef.set(userProfile)
            }
        }
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}