package com.example.smartcare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcare.databinding.ActivityFamilyDashboardBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FamilyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFamilyDashboardBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var taskAdapter: TaskAdapter
    private var connectedLansiaUid: String? = null
    private var selectedDateTime: Calendar = Calendar.getInstance()
    private var isTimeSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        checkConnectionStatus()
        listenForTaskUpdates()

        binding.btnAddTask.setOnClickListener {
            val taskTitle = binding.etNewTask.text.toString().trim()
            if (taskTitle.isNotEmpty()) {
                addTaskToFirestore(taskTitle)
            } else {
                Toast.makeText(this, "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConnect.setOnClickListener {
            val enteredCode = binding.etLansiaCode.text.toString().trim()
            if (enteredCode.isNotEmpty()) {
                findAndConnectToLansia(enteredCode)
            } else {
                Toast.makeText(this, "Kode koneksi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnSetTime.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun checkConnectionStatus() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val uid = document.getString("connectedLansiaUid")
                    if (!uid.isNullOrEmpty()) {
                        connectedLansiaUid = uid
                        binding.tvConnectionStatus.text = "Terhubung dengan Lansia (ID: ...${uid.takeLast(4)})"
                        binding.connectLayout.visibility = View.GONE
                        binding.addTaskLayout.visibility = View.VISIBLE
                    } else {
                        binding.tvConnectionStatus.text = "Belum terhubung dengan akun Lansia."
                        binding.addTaskLayout.visibility = View.GONE
                    }
                } else {
                    binding.tvConnectionStatus.text = "Belum terhubung dengan akun Lansia."
                    binding.addTaskLayout.visibility = View.GONE
                }
            }
    }

    private fun findAndConnectToLansia(code: String) {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                var foundUserUid: String? = null
                for (doc in documents) {
                    if (doc.getString("role") == "lansia" && doc.id.uppercase().startsWith(code.uppercase())) {
                        foundUserUid = doc.id
                        break
                    }
                }

                if (foundUserUid != null) {
                    saveConnection(foundUserUid)
                } else {
                    Toast.makeText(this, "Kode Lansia tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mencari data user.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveConnection(lansiaUid: String) {
        val currentUser = auth.currentUser ?: return
        val userProfileData = mapOf(
            "connectedLansiaUid" to lansiaUid,
            "role" to "keluarga"
        )
        db.collection("users").document(currentUser.uid)
            .set(userProfileData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                checkConnectionStatus()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            mutableListOf(),
            "keluarga",
            onItemClick = { /* Klik item tidak melakukan apa-apa */ },
            onEditClick = { task -> showEditTaskDialog(task) },
            onDeleteClick = { task -> showDeleteConfirmationDialog(task) }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@FamilyDashboardActivity)
            adapter = taskAdapter
        }
    }

    private fun listenForTaskUpdates() {
        val currentUser = auth.currentUser ?: return
        db.collection("tasks")
            .whereEqualTo("createdBy", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FamilyDashboard", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    taskAdapter.updateTasks(snapshots.toObjects())
                }
            }
    }

    private fun showDateTimePicker() {
        val currentDateTime = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDateTime.set(Calendar.YEAR, year)
            selectedDateTime.set(Calendar.MONTH, month)
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)

                val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                binding.tvSelectedTime.text = sdf.format(selectedDateTime.time)
                isTimeSet = true
            }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true).show()

        }, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH), currentDateTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun addTaskToFirestore(title: String) {
        val currentUser = auth.currentUser ?: return
        if (connectedLansiaUid == null) {
            Toast.makeText(this, "Anda harus terhubung dengan akun Lansia terlebih dahulu!", Toast.LENGTH_LONG).show()
            return
        }
        val task = Task(
            title = title,
            createdBy = currentUser.uid,
            assignedTo = connectedLansiaUid!!,
            reminderTime = if (isTimeSet) Timestamp(selectedDateTime.time) else null
        )
        db.collection("tasks").add(task)
            .addOnSuccessListener {
                binding.etNewTask.text.clear()
                binding.tvSelectedTime.text = "Waktu belum diatur"
                isTimeSet = false
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Gagal menambahkan jadwal: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showEditTaskDialog(task: Task) {
        val editText = EditText(this)
        editText.setText(task.title)
        AlertDialog.Builder(this)
            .setTitle("Edit Tugas")
            .setView(editText)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    updateTaskTitle(task, newTitle)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun updateTaskTitle(task: Task, newTitle: String) {
        db.collection("tasks").document(task.id).update("title", newTitle)
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Anda yakin ingin menghapus tugas '${task.title}'?")
            .setPositiveButton("Hapus") { dialog, _ ->
                deleteTask(task)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun deleteTask(task: Task) {
        db.collection("tasks").document(task.id).delete()
    }
}