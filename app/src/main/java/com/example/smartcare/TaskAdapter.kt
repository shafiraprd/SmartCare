package com.example.smartcare

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val role: String,
    private val onItemClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val status: TextView = itemView.findViewById(R.id.tvTaskStatus)
        val editButton: ImageView = itemView.findViewById(R.id.ivEditTask)
        val deleteButton: ImageView = itemView.findViewById(R.id.ivDeleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.title.text = task.title

        // ▼▼▼ PERBAIKAN 1: MENAMPILKAN WAKTU YANG BENAR ▼▼▼
        // Cek apakah ada waktu pengingat yang diatur
        if (task.reminderTime != null) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = sdf.format(task.reminderTime!!.toDate())
            holder.status.text = "Jadwal: $formattedDate"
        } else {
            // Jika tidak ada, tampilkan waktu dibuat
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val formattedDate = sdf.format(task.createdAt.toDate())
            holder.status.text = "Dibuat pada: $formattedDate"
        }

        if (task.isCompleted) {
            holder.status.text = "Status: Selesai"
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // ▼▼▼ PERBAIKAN 2 & 3: LOGIKA UNTUK TUGAS MASA DEPAN ▼▼▼
        val now = Date()
        // Cek apakah tugas ini dijadwalkan untuk masa depan
        val isFutureTask = task.reminderTime != null && task.reminderTime!!.toDate().after(now)

        // Beri efek visual jika tugas belum bisa dikerjakan
        if (isFutureTask && !task.isCompleted && role == "lansia") {
            holder.itemView.alpha = 0.5f // Membuatnya terlihat redup
        } else {
            holder.itemView.alpha = 1.0f // Tampilan normal
        }

        holder.itemView.setOnClickListener {
            if (role == "lansia" && !task.isCompleted) {
                // Lansia hanya bisa menyelesaikan tugas jika waktunya sudah tiba
                if (!isFutureTask) {
                    onItemClick(task)
                } else {
                    Toast.makeText(holder.itemView.context, "Tugas ini belum bisa diselesaikan.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Logika untuk menampilkan tombol edit dan hapus (tetap sama)
        if (role == "keluarga") {
            holder.editButton.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.VISIBLE
            holder.editButton.setOnClickListener { onEditClick(task) }
            holder.deleteButton.setOnClickListener { onDeleteClick(task) }
        } else {
            holder.editButton.visibility = View.GONE
            holder.deleteButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    fun updateTasks(newTasks: List<Task>) {
        taskList.clear()
        taskList.addAll(newTasks)
        notifyDataSetChanged()
    }
}