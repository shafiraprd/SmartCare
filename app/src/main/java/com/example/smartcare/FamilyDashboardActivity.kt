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
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmScheduler = AlarmScheduler(this)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()

        val familyId = auth.currentUser?.uid
        if (familyId == null) {
            goToLogin()
            return
        }
        checkConnectionStatus(familyId)

        binding.fabAddTask.setOnClickListener {
            if (elderlyId != null) {
                showTaskDialog(null)
            } else {
                Toast.makeText(this, "Anda harus terhubung dengan lansia terlebih dahulu.", Toast.LENGTH_SHORT).show()
                promptForElderlyCode()
            }
        }

        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tasks -> {
                    binding.tasksViewContainer.visibility = View.VISIBLE
                    binding.profileViewContainer.visibility = View.GONE
                    supportActionBar?.title = "Dasbor Keluarga"
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
                        } else {
                            Toast.makeText(this, "Gagal mengirim email.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        binding.btnProfileLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadProfileData() {
        binding.tvProfileEmail.text = auth.currentUser?.email ?: "Tidak ditemukan"
    }

    private fun checkConnectionStatus(familyId: String) {
        db.collection("users").document(familyId).get().addOnSuccessListener { document ->
            val connectedId = document.getString("connectedElderlyId")
            if (!connectedId.isNullOrEmpty()) {
                elderlyId = connectedId
                fetchElderlyInfo(connectedId)
                listenForTaskUpdates(connectedId)
            } else {
                promptForElderlyCode()
            }
        }
    }

    private fun fetchElderlyInfo(elderlyId: String) {
        db.collection("users").document(elderlyId).get().addOnSuccessListener { document ->
            binding.tvElderlyEmail.text = document.getString("email") ?: "Email lansia tidak ditemukan"
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            mutableListOf(), "family",
            onItemClick = {},
            onEditClick = { task -> showTaskDialog(task) },
            onDeleteClick = { task -> deleteTask(task) }
        )
        binding.rvTasks.adapter = taskAdapter
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
    }

    private fun listenForTaskUpdates(elderlyId: String) {
        db.collection("users").document(elderlyId).collection("tasks")
            .orderBy("createdAt")
            .addSnapshotListener { snapshots, e ->
                if (e != null) { return@addSnapshotListener }
                val tasks = snapshots?.toObjects(Task::class.java)?.mapIndexed { index, task ->
                    task.apply { id = snapshots.documents[index].id }
                } ?: emptyList()
                taskAdapter.updateTasks(tasks)
            }
    }

    private fun showTaskDialog(task: Task?) {
        val isEditMode = task != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val categoryGroup = dialogView.findViewById<ChipGroup>(R.id.cg_task_category)
        val otherTitleInput = dialogView.findViewById<EditText>(R.id.et_task_title_other)
        val timePickerButton = dialogView.findViewById<Button>(R.id.btn_time_picker)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tv_selected_time)
        val recurrenceGroup = dialogView.findViewById<RadioGroup>(R.id.rg_recurrence)

        categoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            otherTitleInput.visibility = if (checkedIds.contains(R.id.chip_other)) View.VISIBLE else View.GONE
        }

        var selectedHour: Int? = null
        var selectedMinute: Int? = null

        if (isEditMode && task != null) {
            val prefilledChipId = when (task.title) {
                "Minum Obat" -> R.id.chip_medication
                "Makan" -> R.id.chip_eat
                "Janji Temu Dokter" -> R.id.chip_doctor
                "Aktivitas" -> R.id.chip_activity
                else -> R.id.chip_other
            }
            categoryGroup.check(prefilledChipId)
            if (prefilledChipId == R.id.chip_other) {
                otherTitleInput.setText(task.title)
            }
        }

        timePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                tvSelectedTime.text = String.format("Pengingat: %02d:%02d", hourOfDay, minute)
                tvSelectedTime.visibility = View.VISIBLE
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEditMode) "Edit Tugas" else "Tambah Tugas Baru")
            .setView(dialogView)
            .setPositiveButton(if (isEditMode) "Simpan" else "Tambah") { dialog, _ ->
                val checkedChipId = categoryGroup.checkedChipId
                val taskTitle = if (checkedChipId == View.NO_ID) ""
                else if (checkedChipId == R.id.chip_other) otherTitleInput.text.toString().trim()
                else dialogView.findViewById<Chip>(checkedChipId).text.toString()

                if (taskTitle.isEmpty()) {
                    Toast.makeText(this, "Pilih kategori atau isi judul tugas.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                var reminderTime: Timestamp? = null
                if (selectedHour != null && selectedMinute != null) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour!!)
                        set(Calendar.MINUTE, selectedMinute!!)
                        set(Calendar.SECOND, 0)
                    }
                    if (calendar.timeInMillis < System.currentTimeMillis() && !isEditMode) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    reminderTime = Timestamp(calendar.time)
                }

                val selectedRecurrenceId = recurrenceGroup.checkedRadioButtonId
                val recurrence = if (selectedRecurrenceId == R.id.rb_daily) "daily" else "none"

                if (recurrence == "daily" && reminderTime == null) {
                    Toast.makeText(this, "Tugas harian harus memiliki waktu pengingat.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (isEditMode) {
                    updateTask(task!!, taskTitle, reminderTime, recurrence)
                } else {
                    addTask(taskTitle, reminderTime, recurrence)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun addTask(taskTitle: String, reminderTime: Timestamp?, recurrence: String) {
        val familyId = auth.currentUser?.uid ?: return
        if (elderlyId == null) { return }
        val newTask = hashMapOf(
            "title" to taskTitle, "createdBy" to familyId, "assignedTo" to elderlyId!!,
            "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
            "reminderTime" to reminderTime, "recurrence" to recurrence
        )
        db.collection("users").document(elderlyId!!).collection("tasks").add(newTask)
            .addOnSuccessListener { docRef ->
                if (reminderTime != null) {
                    val createdTask = Task(id = docRef.id, title = taskTitle, reminderTime = reminderTime, recurrence = recurrence)
                    alarmScheduler.schedule(createdTask)
                }
            }
    }

    private fun updateTask(task: Task, newTitle: String, newReminderTime: Timestamp?, newRecurrence: String) {
        val taskRef = db.collection("users").document(elderlyId!!).collection("tasks").document(task.id)
        val updatedData = mapOf("title" to newTitle, "reminderTime" to newReminderTime, "recurrence" to newRecurrence)
        taskRef.update(updatedData)
            .addOnSuccessListener {
                alarmScheduler.cancel(task)
                if (newReminderTime != null) {
                    val updatedTask = task.copy(title = newTitle, reminderTime = newReminderTime, recurrence = newRecurrence)
                    alarmScheduler.schedule(updatedTask)
                }
            }
    }

    private fun deleteTask(task: Task) {
        alarmScheduler.cancel(task)
        db.collection("users").document(elderlyId!!).collection("tasks").document(task.id).delete()
    }

    private fun promptForElderlyCode() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hubungkan dengan Lansia")
        builder.setMessage("Masukkan kode koneksi unik.")
        val input = EditText(this)
        builder.setView(input)
        builder.setPositiveButton("Hubungkan") { dialog, _ ->
            connectWithElderly(input.text.toString().trim())
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun connectWithElderly(code: String) {
        if (code.isEmpty()) return
        db.collection("users").whereEqualTo("connectionCode", code).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Kode tidak ditemukan.", Toast.LENGTH_SHORT).show()
                } else {
                    val foundElderlyId = documents.documents.first().id
                    val familyId = auth.currentUser?.uid!!
                    db.collection("users").document(familyId).update("connectedElderlyId", foundElderlyId)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                            checkConnectionStatus(familyId)
                        }
                }
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