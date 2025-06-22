// Halaman ini digunakan untuk menampilkan dan mengelola daftar tugas untuk seorang lansia dari sisi anggota keluarga
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

    // Menghubungkan tampilan XML dengan logika Kotlin
    private lateinit var binding: ActivityTaskListBinding

    // Referensi ke Firestore database
    private val db = Firebase.firestore

    // Untuk mengecek siapa pengguna yang login sekarang
    private val auth = Firebase.auth

    // Menyimpan ID lansia yang sedang dikelola tugasnya
    private var elderlyId: String? = null

    // Adapter yang bertugas menampilkan data ke dalam RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    // Untuk menjadwalkan alarm pengingat (di sisi perangkat lansia)
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi object alarm scheduler
        alarmScheduler = AlarmScheduler(this)

        // Ambil data ID dan nama lansia dari intent yang dikirim dari halaman sebelumnya
        elderlyId = intent.getStringExtra("ELDERLY_ID")
        val elderlyDisplayName = intent.getStringExtra("ELDERLY_DISPLAY_NAME")

        // Jika ID tidak ditemukan, tutup halaman ini karena tidak bisa lanjut
        if (elderlyId == null) {
            Toast.makeText(this, "ID Lansia tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Atur tampilan toolbar di bagian atas
        setSupportActionBar(binding.toolbarTaskList)
        supportActionBar?.title = "Tugas untuk: $elderlyDisplayName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Siapkan tampilan list tugas (RecyclerView)
        setupRecyclerView()

        // Dengarkan perubahan data dari database secara real-time
        listenForTaskUpdates(elderlyId!!)

        // Jika tombol tambah tugas ditekan, tampilkan dialog input
        binding.fabAddTask.setOnClickListener { showTaskDialog(null) }
    }

    // Saat tombol back di toolbar ditekan
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Menyiapkan RecyclerView dan adapter
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

    // Mendengarkan data tugas dari Firestore secara langsung
    private fun listenForTaskUpdates(elderlyId: String) {
        db.collection("users").document(elderlyId).collection("tasks")
            .orderBy("createdAt")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val tasks = snapshots?.toObjects(Task::class.java)?.mapIndexed { index, task ->
                    task.apply { id = snapshots.documents[index].id }
                } ?: emptyList()
                taskAdapter.updateTasks(tasks)
            }
    }

    // Menampilkan dialog untuk input tugas baru atau edit tugas lama
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

        // Ganti hint input berdasarkan kategori yang dipilih
        categoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            notesLayout.hint = if (checkedIds.contains(R.id.chip_other)) "Judul Tugas Lainnya (wajib diisi)" else "Catatan (opsional)"
        }

        val selectedCalendar = Calendar.getInstance()
        var dateSet = false
        var timeSet = false

        // Kalau sedang edit tugas, bisa isi nilai-nilai lama ke dialog (belum diisi)
        if (isEditMode && task != null) {
            // Kosong untuk sekarang
        }

        // Ketika tombol pilih tanggal ditekan
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

        // Ketika tombol pilih jam ditekan
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

        // Tampilkan kotak dialog ke layar
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

                val (taskTitle, taskNotes) = when (selectedChip.id) {
                    R.id.chip_other -> {
                        if (notes.isEmpty()) {
                            Toast.makeText(this, "Judul tugas 'Lainnya' wajib diisi.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        Pair(notes, "")
                    }
                    else -> Pair(selectedChip.text.toString(), notes)
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

    // Menambahkan tugas baru ke Firestore
    private fun addTask(title: String, notes: String, reminderTime: Timestamp?, recurrence: String) {
        val familyId = auth.currentUser?.uid ?: return
        val newTask = hashMapOf(
            "title" to title,
            "notes" to notes,
            "createdBy" to familyId,
            "assignedTo" to elderlyId!!,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "reminderTime" to reminderTime,
            "recurrence" to recurrence
        )
        db.collection("users").document(elderlyId!!).collection("tasks").add(newTask)
    }

    // Memperbarui tugas lama di Firestore
    private fun updateTask(task: Task, title: String, notes: String, newReminderTime: Timestamp?, newRecurrence: String) {
        val taskRef = db.collection("users").document(elderlyId!!).collection("tasks").document(task.id)
        val updatedData = mapOf(
            "title" to title,
            "notes" to notes,
            "reminderTime" to newReminderTime,
            "recurrence" to newRecurrence
        )
        taskRef.update(updatedData)
    }

    // Menghapus tugas dari Firestore
    private fun deleteTask(task: Task) {
        db.collection("users").document(elderlyId!!).collection("tasks").document(task.id).delete()
    }
}
