package com.example.smartcare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.smartcare.databinding.ActivityTaskListBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var elderlyId: String? = null
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        alarmScheduler = AlarmScheduler(this)

        elderlyId = intent.getStringExtra("ELDERLY_ID")
        val elderlyDisplayName = intent.getStringExtra("ELDERLY_DISPLAY_NAME")

        if (elderlyId == null) {
            Toast.makeText(this, "ID Lansia tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbarTaskList)
        supportActionBar?.title = "Tugas untuk: $elderlyDisplayName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        listenForTaskUpdates(elderlyId!!)
        binding.fabAddTask.setOnClickListener { showTaskDialog(null) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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
        val notesLayout = dialogView.findViewById<TextInputLayout>(R.id.til_task_notes)
        val notesInput = dialogView.findViewById<EditText>(R.id.et_task_notes)
        val datePickerButton = dialogView.findViewById<Button>(R.id.btn_date_picker)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val timePickerButton = dialogView.findViewById<Button>(R.id.btn_time_picker)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tv_selected_time)
        val recurrenceGroup = dialogView.findViewById<RadioGroup>(R.id.rg_recurrence)

        categoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            notesLayout.hint = if (checkedIds.contains(R.id.chip_other)) "Judul Tugas Lainnya (wajib diisi)" else "Catatan (opsional)"
        }

        val selectedCalendar = Calendar.getInstance()
        var dateSet = false
        var timeSet = false

        if (isEditMode && task != null) {
            // Logika untuk mengisi dialog saat mode edit
        }

        datePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                tvSelectedDate.text = sdf.format(selectedCalendar.time)
                tvSelectedDate.visibility = View.VISIBLE
                dateSet = true
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        timePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                tvSelectedTime.text = String.format("%02d:%02d", hourOfDay, minute)
                tvSelectedTime.visibility = View.VISIBLE
                timeSet = true
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEditMode) "Edit Tugas" else "Tambah Tugas Baru")
            .setView(dialogView)
            .setPositiveButton(if (isEditMode) "Simpan" else "Tambah") { _, _ ->
                val checkedChipId = categoryGroup.checkedChipId
                if (checkedChipId == View.NO_ID) {
                    Toast.makeText(this, "Silakan pilih jenis tugas.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val notes = notesInput.text.toString().trim()
                val selectedChip = dialogView.findViewById<Chip>(checkedChipId)

                // DIUBAH: Menggunakan 'when' sebagai expression untuk menjamin inisialisasi
                val (taskTitle, taskNotes) = when (selectedChip.id) {
                    R.id.chip_other -> {
                        if (notes.isEmpty()) {
                            Toast.makeText(this, "Judul tugas 'Lainnya' wajib diisi.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        // Untuk 'Lainnya', judul diambil dari input, catatan dikosongkan
                        Pair(notes, "")
                    }
                    else -> {
                        // Untuk kategori lain, judul diambil dari chip, catatan dari input
                        Pair(selectedChip.text.toString(), notes)
                    }
                }

                var reminderTime: Timestamp? = null
                if (dateSet && timeSet) {
                    if (selectedCalendar.timeInMillis < System.currentTimeMillis() && !isEditMode) {
                        Toast.makeText(this, "Waktu pengingat tidak boleh di masa lalu.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    reminderTime = Timestamp(selectedCalendar.time)
                } else if (dateSet || timeSet) {
                    Toast.makeText(this, "Silakan pilih tanggal dan jam secara lengkap.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val recurrence = if (recurrenceGroup.checkedRadioButtonId == R.id.rb_daily) "daily" else "none"
                if (recurrence == "daily" && reminderTime == null) {
                    Toast.makeText(this, "Tugas harian harus memiliki waktu pengingat.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (isEditMode) {
                    updateTask(task!!, taskTitle, taskNotes, reminderTime, recurrence)
                } else {
                    addTask(taskTitle, taskNotes, reminderTime, recurrence)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun addTask(title: String, notes: String, reminderTime: Timestamp?, recurrence: String) {
        val familyId = auth.currentUser?.uid ?: return
        val newTask = hashMapOf(
            "title" to title, "notes" to notes, "createdBy" to familyId, "assignedTo" to elderlyId!!,
            "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
            "reminderTime" to reminderTime, "recurrence" to recurrence
        )
        db.collection("users").document(elderlyId!!).collection("tasks").add(newTask)
            .addOnSuccessListener { docRef ->
                if (reminderTime != null) {
                    val createdTask = Task(id = docRef.id, title = title, notes = notes, reminderTime = reminderTime, recurrence = recurrence)
                    alarmScheduler.schedule(createdTask)
                }
            }
    }

    private fun updateTask(task: Task, title: String, notes: String, newReminderTime: Timestamp?, newRecurrence: String) {
        val taskRef = db.collection("users").document(elderlyId!!).collection("tasks").document(task.id)
        val updatedData = mapOf("title" to title, "notes" to notes, "reminderTime" to newReminderTime, "recurrence" to newRecurrence)
        taskRef.update(updatedData)
            .addOnSuccessListener {
                alarmScheduler.cancel(task)
                if (newReminderTime != null) {
                    val updatedTask = task.copy(title = title, notes = notes, reminderTime = newReminderTime, recurrence = newRecurrence)
                    alarmScheduler.schedule(updatedTask)
                }
            }
    }

    private fun deleteTask(task: Task) {
        alarmScheduler.cancel(task)
        db.collection("users").document(elderlyId!!).collection("tasks").document(task.id).delete()
    }
}