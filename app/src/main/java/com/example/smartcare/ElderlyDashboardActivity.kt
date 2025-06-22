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

// Kelas ini mengatur semua logika untuk halaman Dashboard Lansia.
class ElderlyDashboardActivity : AppCompatActivity() {

    // Variabel untuk mengakses elemen-elemen UI dari layout (ViewBinding).
    private lateinit var binding: ActivityElderlyDashboardBinding
    // Inisialisasi koneksi ke database Firestore.
    private val db = Firebase.firestore
    // Inisialisasi koneksi ke layanan Autentikasi Firebase.
    private val auth = Firebase.auth
    // Adapter yang akan menjadi jembatan antara data dan RecyclerView.
    private lateinit var taskAdapter: TaskAdapter
    // Daftar lokal untuk menyimpan data tugas, memudahkan manipulasi.
    private val tasksList = mutableListOf<Task>()
    // Kelas pembantu untuk menjadwalkan dan membatalkan alarm.
    private lateinit var alarmScheduler: AlarmScheduler

    // Fungsi onCreate adalah yang pertama kali dijalankan saat halaman ini dibuka.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menyiapkan ViewBinding agar bisa digunakan.
        binding = ActivityElderlyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi penjadwal alarm dengan konteks dari activity ini.
        alarmScheduler = AlarmScheduler(this)
        // Mengatur toolbar kustom sebagai Action Bar utama untuk halaman ini.
        setSupportActionBar(binding.toolbarElderly)

        // Memanggil fungsi untuk menyiapkan RecyclerView.
        setupRecyclerView()

        // Dapatkan ID pengguna yang sedang login.
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Jika tidak ada yang login, paksa kembali ke halaman login dan hentikan eksekusi.
            goToLogin()
            return
        }

        // Panggil fungsi untuk mengambil data-data awal yang dibutuhkan.
        fetchConnectionCode(userId)
        listenForTaskUpdates(userId)

        // Mengatur listener untuk Bottom Navigation, agar tahu menu mana yang di-klik.
        binding.bottomNavViewElderly.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Jika menu 'Tugas' (navigation_tasks) diklik...
                R.id.navigation_tasks -> {
                    // Tampilkan kontainer tugas dan sembunyikan kontainer profil.
                    binding.tasksViewContainer.visibility = View.VISIBLE
                    binding.profileViewContainer.visibility = View.GONE
                    // Ubah judul di toolbar.
                    supportActionBar?.title = "Dasbor Tugas Anda"
                    true
                }
                // Jika menu 'Profil' (navigation_profile) diklik...
                R.id.navigation_profile -> {
                    // Sembunyikan kontainer tugas dan tampilkan kontainer profil.
                    binding.tasksViewContainer.visibility = View.GONE
                    binding.profileViewContainer.visibility = View.VISIBLE
                    // Ubah judul di toolbar.
                    supportActionBar?.title = "Profil"
                    // Panggil fungsi untuk memuat data profil.
                    loadProfileData()
                    true
                }
                else -> false
            }
        }

        // Memberi fungsi pada tombol-tombol di dalam Tampilan Profil.
        binding.btnChangePassword.setOnClickListener {
            val user = auth.currentUser
            // Pastikan email pengguna ada sebelum mengirim email reset.
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

    // Fungsi sederhana untuk mengambil dan menampilkan email pengguna di halaman profil.
    private fun loadProfileData() {
        binding.tvProfileEmail.text = auth.currentUser?.email ?: "Tidak ditemukan"
    }

    // Menyiapkan RecyclerView dan menghubungkannya dengan TaskAdapter.
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

    /**
     * Fungsi ini dipanggil saat pengguna LANSIA mengklik sebuah item tugas.
     * Logika di sini sangat penting untuk menentukan apakah sebuah tugas bisa diselesaikan atau tidak.
     */
    private fun handleTaskCompletion(task: Task) {
        // Cari posisi tugas yang diklik di dalam daftar lokal.
        val position = tasksList.indexOfFirst { it.id == task.id }
        if (position == -1) return // Jika tidak ditemukan, hentikan.

        // Jika tugas sudah selesai atau terlewat, jangan lakukan apa-apa, cukup refresh tampilannya.
        if (task.status == "completed" || task.status == "missed") {
            Toast.makeText(this, "Tugas ini sudah selesai atau terlewat.", Toast.LENGTH_SHORT).show()
            taskAdapter.notifyItemChanged(position)
            return
        }

        // Jika tugas tidak punya waktu pengingat, bisa langsung diselesaikan kapan saja.
        val reminder = task.reminderTime
        if (reminder == null) {
            updateTaskStatus(task, "completed")
            return
        }

        // Ambil waktu saat ini, waktu pengingat, dan hitung batas waktu (deadline).
        // Deadline diatur 15 menit setelah waktu pengingat.
        val currentTime = Date().time
        val reminderTime = reminder.toDate().time
        val deadline = reminderTime + (15 * 60 * 1000) // 15 menit dalam milidetik

        when {
            // KASUS 1: Waktu belum tiba. Pengguna belum boleh menyelesaikan tugas.
            currentTime < reminderTime -> {
                Toast.makeText(this, "Belum waktunya menyelesaikan tugas ini.", Toast.LENGTH_SHORT).show()
                taskAdapter.notifyItemChanged(position)
            }
            // KASUS 2: Waktu sudah lewat dari deadline. Tugas dianggap terlewat.
            currentTime > deadline -> {
                Toast.makeText(this, "Waktu untuk menyelesaikan tugas ini sudah lewat.", Toast.LENGTH_SHORT).show()
                // Status akan diubah menjadi 'missed' oleh fungsi checkForMissedTasks secara otomatis.
                taskAdapter.notifyItemChanged(position)
            }
            // KASUS 3: Pengguna mengklik di dalam jendela waktu yang tepat (0-15 menit setelah pengingat).
            else -> {
                updateTaskStatus(task, "completed")
            }
        }
    }

    /**
     * Fungsi ini 'menguping' perubahan data di Firestore secara real-time.
     * Path-nya sekarang lebih spesifik: hanya mengambil tugas dari sub-koleksi milik pengguna ini.
     */
    private fun listenForTaskUpdates(userId: String) {
        db.collection("users").document(userId).collection("tasks")
            .orderBy("createdAt")
            .addSnapshotListener { snapshots, e ->
                if (e != null) { return@addSnapshotListener }
                val tasks = snapshots?.toObjects(Task::class.java)?.mapIndexed { index, task ->
                    // Memasukkan ID dokumen ke dalam objek Task agar mudah diakses.
                    task.apply { id = snapshots.documents[index].id }
                } ?: emptyList()

                // --- INI DIA BAGIAN BARUNYA ---
                // Oke, sekarang aku berada di perangkat lansia. Ini tempat yang tepat untuk mengatur alarm.
                // Aku akan cek satu per satu semua tugas yang diterima dari database.
                tasks.forEach { task ->
                    // Aku hanya akan menyetel alarm JIKA tugasnya punya waktu pengingat DAN statusnya masih 'pending'.
                    if (task.status == "pending" && task.reminderTime != null) {
                        // Jika syarat terpenuhi, panggil fungsi untuk menjadwalkan alarm.
                        alarmScheduler.schedule(task)
                    } else {
                        // Jika tugas sudah tidak 'pending' (misal: selesai atau terlewat),
                        // pastikan alarmnya dibatalkan untuk mencegah notifikasi yang tidak perlu.
                        alarmScheduler.cancel(task)
                    }
                }
                // --- AKHIR DARI BAGIAN BARU ---

                // Sebelum menampilkan, cek dulu apakah ada tugas yang terlewat.
                checkForMissedTasks(tasks, userId)
            }
    }

    /**
     * Fungsi ini memeriksa semua tugas untuk melihat apakah ada yang terlewat.
     * Ini penting agar status tugas selalu up-to-date bahkan jika aplikasi dibuka setelah waktu deadline.
     */
    private fun checkForMissedTasks(tasks: List<Task>, userId: String) {
        // 'batch' digunakan untuk melakukan beberapa operasi tulis ke database sekaligus, lebih efisien.
        val batch = db.batch()
        val currentTime = Date().time
        val tasksToUpdate = mutableListOf<Task>()
        var needsDbUpdate = false

        for (task in tasks) {
            val reminder = task.reminderTime
            // Cek tugas yang masih 'pending' dan punya waktu pengingat.
            if (task.status == "pending" && reminder != null) {
                val deadline = reminder.toDate().time + (15 * 60 * 1000)
                if (currentTime > deadline) {
                    // Jika terlewat, tandai tugas tersebut untuk diubah menjadi 'missed' di dalam batch.
                    val taskRef = db.collection("users").document(userId).collection("tasks").document(task.id)
                    batch.update(taskRef, "status", "missed")
                    tasksToUpdate.add(task)
                    needsDbUpdate = true
                }
            }
        }

        // Jika ada tugas yang perlu diupdate...
        if (needsDbUpdate) {
            // Jalankan semua operasi di dalam batch.
            batch.commit().addOnCompleteListener {
                // Setelah berhasil, batalkan semua alarm untuk tugas yang sudah terlewat.
                tasksToUpdate.forEach { alarmScheduler.cancel(it) }
                // Listener akan otomatis terpanggil lagi setelah update ini, jadi tidak perlu panggil adapter di sini.
            }
        } else {
            // Jika tidak ada yang perlu di-update di DB, langsung perbarui UI.
            taskAdapter.updateTasks(tasks)
        }
    }

    // Fungsi ini untuk mengubah status sebuah tugas di Firestore (misalnya, dari 'pending' ke 'completed').
    private fun updateTaskStatus(task: Task, newStatus: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("tasks").document(task.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (newStatus == "completed") {
                    Toast.makeText(this, "Tugas '${task.title}' selesai!", Toast.LENGTH_SHORT).show()
                    // Jika tugas berhasil diselesaikan, batalkan alarmnya agar tidak berbunyi lagi.
                    // Pembatalan di sini penting untuk respons instan saat user klik.
                    alarmScheduler.cancel(task)
                }
            }
    }

    // Mengambil kode koneksi unik dari profil pengguna.
    private fun fetchConnectionCode(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            binding.tvConnectionCode.text = document.getString("connectionCode") ?: "Kode belum dibuat"
        }
    }

    // Fungsi untuk keluar dari akun dan kembali ke halaman login.
    private fun logoutUser() {
        auth.signOut()
        goToLogin()
    }

    // Fungsi pembantu untuk navigasi ke LoginActivity.
    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}