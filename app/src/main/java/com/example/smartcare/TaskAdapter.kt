package com.example.smartcare

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

// Adapter ini digunakan untuk menampilkan daftar tugas (task) dalam RecyclerView
class TaskAdapter(
    private var tasks: MutableList<Task>, // list tugas yang akan ditampilkan
    private val userRole: String, // peran pengguna: "family" atau "elderly"
    private val onItemClick: (Task) -> Unit, // dipanggil saat checkbox diklik
    private val onEditClick: (Task) -> Unit, // dipanggil saat tombol edit diklik
    private val onDeleteClick: (Task) -> Unit // dipanggil saat tombol hapus diklik
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // ViewHolder adalah komponen yang memegang view dari tiap item
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivTaskIcon: ImageView = itemView.findViewById(R.id.iv_task_icon)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val tvTaskNotes: TextView = itemView.findViewById(R.id.tv_task_notes)
        val tvTaskStatus: TextView = itemView.findViewById(R.id.tv_task_status)
        val cbTaskDone: CheckBox = itemView.findViewById(R.id.cb_task_done)
        val ivEditTask: ImageButton = itemView.findViewById(R.id.iv_edit_task)
        val ivDeleteTask: ImageButton = itemView.findViewById(R.id.iv_delete_task)
        val tvTaskReminderTime: TextView = itemView.findViewById(R.id.tv_task_reminder_time)
    }

    // Membuat tampilan layout untuk tiap item di daftar
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    // Mengisi data ke tampilan item berdasarkan posisi
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Tampilkan judul dan status tugas
        holder.tvTaskTitle.text = task.title
        holder.tvTaskStatus.text = "Status: ${task.status}"

        // Jika catatan ada isinya, tampilkan. Kalau kosong, disembunyikan.
        if (task.notes.isNotEmpty()) {
            holder.tvTaskNotes.text = task.notes
            holder.tvTaskNotes.visibility = View.VISIBLE
        } else {
            holder.tvTaskNotes.visibility = View.GONE
        }

        // Menentukan ikon berdasarkan isi judul tugas (pakai kata kunci sederhana)
        val taskTitleLower = task.title.lowercase(Locale.ROOT)
        val iconResId = when {
            "obat" in taskTitleLower -> R.drawable.ic_task_medication
            "makan" in taskTitleLower -> R.drawable.ic_task_food
            "dokter" in taskTitleLower || "janji" in taskTitleLower || "kontrol" in taskTitleLower -> R.drawable.ic_task_doctor
            "aktivitas" in taskTitleLower || "jalan" in taskTitleLower || "olahraga" in taskTitleLower -> R.drawable.ic_task_activity
            else -> R.drawable.ic_task_default
        }
        holder.ivTaskIcon.setImageResource(iconResId)

        // Tampilkan waktu pengingat jika ada
        task.reminderTime?.let { timestamp ->
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.tvTaskReminderTime.text = "Pengingat: ${sdf.format(timestamp.toDate())}"
            holder.tvTaskReminderTime.visibility = View.VISIBLE
        } ?: run {
            holder.tvTaskReminderTime.visibility = View.GONE
        }

        // Tampilkan atau sembunyikan tombol berdasarkan peran pengguna
        if (userRole == "family") {
            // Keluarga tidak bisa mencentang checkbox, tapi bisa edit dan hapus
            holder.cbTaskDone.visibility = View.GONE
            holder.ivEditTask.visibility = View.VISIBLE
            holder.ivDeleteTask.visibility = View.VISIBLE
        } else { // lansia
            // Lansia bisa centang tugas, tapi tidak bisa edit atau hapus
            holder.cbTaskDone.visibility = View.VISIBLE
            holder.ivEditTask.visibility = View.GONE
            holder.ivDeleteTask.visibility = View.GONE

            // Font diperbesar supaya lebih mudah dibaca
            holder.tvTaskTitle.textSize = 20f
            holder.tvTaskReminderTime.textSize = 16f
            holder.tvTaskStatus.textSize = 16f
            holder.cbTaskDone.textSize = 18f
        }

        // Kalau status tugas "completed", tampilkan garis coretan dan redupkan warna teks
        if (task.status == "completed") {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskTitle.alpha = 0.5f
        } else {
            // Kalau belum selesai, tampilkan normal
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskTitle.alpha = 1.0f
        }

        // Atur status checkbox sesuai status tugas
        holder.cbTaskDone.isChecked = task.status == "completed"

        // Atur listener saat tombol diklik
        holder.ivEditTask.setOnClickListener { onEditClick(task) }
        holder.ivDeleteTask.setOnClickListener { onDeleteClick(task) }
        holder.cbTaskDone.setOnClickListener { onItemClick(task) } // checkbox dipakai untuk update status
    }

    // Jumlah item yang akan ditampilkan
    override fun getItemCount(): Int = tasks.size

    // Fungsi untuk memperbarui data tugas yang ditampilkan
    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}
