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

// Ini adalah halaman untuk anggota keluarga melihat dan mengelola tugas untuk seorang lansia.
class TaskListActivity : AppCompatActivity() {

    // Oke, ini semua variabel yang aku butuhkan untuk halaman ini.
    private lateinit var binding: ActivityTaskListBinding // Untuk mengakses elemen UI.
    private val db = Firebase.firestore // Koneksi ke database Firestore.
    private val auth = Firebase.auth // Untuk informasi user yang login.
    private var elderlyId: String? = null // Untuk menyimpan ID lansia yang sedang dilihat.
    private lateinit var taskAdapter: TaskAdapter // Adapter untuk menampilkan daftar tugas.
    private lateinit var alarmScheduler: AlarmScheduler // Objek untuk mengatur alarm.

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fungsi ini dijalankan saat halaman pertama kali dibuat.
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi AlarmScheduler.
        alarmScheduler = AlarmScheduler(this)

        // Aku akan ambil ID dan nama lansia yang dikirim dari halaman sebelumnya.
        elderlyId = intent.getStringExtra("ELDERLY_ID")
        val elderlyDisplayName = intent.getStringExtra("ELDERLY_DISPLAY_NAME")

        // Kalau ID lansia tidak ada, ini masalah. Tampilkan pesan error dan tutup halaman.
        if (elderlyId == null) {
            Toast.makeText(this, "ID Lansia tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Atur toolbar di bagian atas halaman.
        setSupportActionBar(binding.toolbarTaskList)
        supportActionBar?.title = "Tugas untuk: $elderlyDisplayName" // Tampilkan nama lansia di judul.
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Tambahkan tombol kembali.

        // Siapkan RecyclerView untuk menampilkan daftar tugas.
        setupRecyclerView()
        // Mulai 'mendengarkan' perubahan data tugas dari Firestore.
        listenForTaskUpdates(elderlyId!!)
        // Atur aksi untuk tombol FAB (+) untuk menambah tugas baru.
        binding.fabAddTask.setOnClickListener { showTaskDialog(null) }
    }

    // Kalau tombol kembali di toolbar ditekan, tutup halaman ini.
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Fungsi untuk menyiapkan RecyclerView.
    private fun setupRecyclerView() {
        // Buat adapter dengan logika untuk klik edit dan hapus.
        taskAdapter = TaskAdapter(
            mutableListOf(), "family",
            onItemClick = {}, // Tidak ada aksi saat item di-klik biasa.
            onEditClick = { task -> showTaskDialog(task) }, // Kalau ikon edit di-klik, tampilkan dialog edit.
            onDeleteClick = { task -> deleteTask(task) } // Kalau ikon hapus di-klik, hapus tugas.
        )
        // Hubungkan adapter dan layout manager ke RecyclerView.
        binding.rvTasks.adapter = taskAdapter
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
    }

    // Fungsi ini terus memantau koleksi 'tasks' di Firestore.
    private fun listenForTaskUpdates(elderlyId: String) {
        db.collection("users").document(elderlyId).collection("tasks")
            .orderBy("createdAt") // Urutkan berdasarkan waktu pembuatan.
            .addSnapshotListener { snapshots, e ->
                // Kalau ada error, hentikan.
                if (e != null) { return@addSnapshotListener }
                // Ubah data dari Firestore menjadi daftar objek Task.
                val tasks = snapshots?.toObjects(Task::class.java)?.mapIndexed { index, task ->
                    task.apply { id = snapshots.documents[index].id }
                } ?: emptyList()
                // Perbarui data di adapter agar tampilan di layar juga ikut berubah.
                taskAdapter.updateTasks(tasks)
            }
    }

    // Menampilkan dialog untuk menambah atau mengedit tugas.
    private fun showTaskDialog(task: Task?) {
        // Kalau 'task' tidak null, berarti ini mode edit.
        val isEditMode = task != null
        // Siapkan layout untuk dialog.
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)

        // Inisialisasi semua elemen UI di dalam dialog.
        val categoryGroup = dialogView.findViewById<ChipGroup>(R.id.cg_task_category)
        val notesLayout = dialogView.findViewById<TextInputLayout>(R.id.til_task_notes)
        val notesInput = dialogView.findViewById<EditText>(R.id.et_task_notes)
        val datePickerButton = dialogView.findViewById<Button>(R.id.btn_date_picker)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val timePickerButton = dialogView.findViewById<Button>(R.id.btn_time_picker)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tv_selected_time)
        val recurrenceGroup = dialogView.findViewById<RadioGroup>(R.id.rg_recurrence)

        // ... (Logika dialog lainnya tidak perlu diubah)
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

        // Tampilkan dialognya.
        AlertDialog.Builder(this)
            .setTitle(if (isEditMode) "Edit Tugas" else "Tambah Tugas Baru")
            .setView(dialogView)
            .setPositiveButton(if (isEditMode) "Simpan" else "Tambah") { _, _ ->
                // Logika saat tombol 'Tambah' atau 'Simpan' ditekan.
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
                    else -> {
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

                // Tentukan apakah akan menambah tugas baru atau memperbarui yang sudah ada.
                if (isEditMode) {
                    updateTask(task!!, taskTitle, taskNotes, reminderTime, recurrence)
                } else {
                    addTask(taskTitle, taskNotes, reminderTime, recurrence)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Fungsi untuk menambah tugas baru ke Firestore.
    private fun addTask(title: String, notes: String, reminderTime: Timestamp?, recurrence: String) {
        // Dapatkan ID pengguna (keluarga) yang sedang login.
        val familyId = auth.currentUser?.uid ?: return
        // Siapkan data tugas yang akan disimpan.
        val newTask = hashMapOf(
            "title" to title, "notes" to notes, "createdBy" to familyId, "assignedTo" to elderlyId!!,
            "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
            "reminderTime" to reminderTime, "recurrence" to recurrence
        )
        // Simpan data ke Firestore.
        db.collection("users").document(elderlyId!!).collection("tasks").add(newTask)
            .addOnSuccessListener { docRef ->
                // DIHAPUS: Penjadwalan alarm dari sini salah.
                // Logika alarm dipindahkan ke ElderlyDashboardActivity agar alarm
                // disetel di perangkat yang benar (perangkat lansia).
                // if (reminderTime != null) {
                //     val createdTask = Task(id = docRef.id, title = title, notes = notes, reminderTime = reminderTime, recurrence = recurrence)
                //     alarmScheduler.schedule(createdTask)
                // }
            }
    }

    // Fungsi untuk memperbarui tugas yang ada di Firestore.
    private fun updateTask(task: Task, title: String, notes: String, newReminderTime: Timestamp?, newRecurrence: String) {
        val taskRef = db.collection("users").document(elderlyId!!).collection("tasks").document(task.id)
        val updatedData = mapOf("title" to title, "notes" to notes, "reminderTime" to newReminderTime, "recurrence" to newRecurrence)
        taskRef.update(updatedData)
            .addOnSuccessListener {
                // DIHAPUS: Logika pembatalan dan penjadwalan ulang alarm juga dipindahkan
                // ke ElderlyDashboardActivity untuk menjaga konsistensi.
                // alarmScheduler.cancel(task)
                // if (newReminderTime != null) {
                //     val updatedTask = task.copy(title = title, notes = notes, reminderTime = newReminderTime, recurrence = newRecurrence)
                //     alarmScheduler.schedule(updatedTask)
                // }
            }
    }

    // Fungsi untuk menghapus tugas.
    private fun deleteTask(task: Task) {
        // DIHAPUS: Pembatalan alarm juga seharusnya terjadi di sisi lansia saat
        // data tugas hilang dari daftar mereka. Ini untuk memastikan semua logika alarm
        // ada di satu tempat.
        // alarmScheduler.cancel(task)
        db.collection("users").document(elderlyId!!).collection("tasks").document(task.id).delete()
    }
}