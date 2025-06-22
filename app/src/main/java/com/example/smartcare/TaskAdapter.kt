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

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val userRole: String,
    private val onItemClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivTaskIcon: ImageView = itemView.findViewById(R.id.iv_task_icon)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val tvTaskStatus: TextView = itemView.findViewById(R.id.tv_task_status)
        val cbTaskDone: CheckBox = itemView.findViewById(R.id.cb_task_done)
        val ivEditTask: ImageButton = itemView.findViewById(R.id.iv_edit_task)
        val ivDeleteTask: ImageButton = itemView.findViewById(R.id.iv_delete_task)
        val tvTaskReminderTime: TextView = itemView.findViewById(R.id.tv_task_reminder_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvTaskTitle.text = task.title
        holder.tvTaskStatus.text = "Status: ${task.status}"

        val taskTitleLower = task.title.lowercase(Locale.ROOT)
        val iconResId = when {
            "obat" in taskTitleLower -> R.drawable.ic_task_medication
            "makan" in taskTitleLower -> R.drawable.ic_task_food
            "dokter" in taskTitleLower || "janji" in taskTitleLower || "kontrol" in taskTitleLower -> R.drawable.ic_task_doctor
            "aktivitas" in taskTitleLower || "jalan" in taskTitleLower || "olahraga" in taskTitleLower -> R.drawable.ic_task_activity
            else -> R.drawable.ic_task_default
        }
        holder.ivTaskIcon.setImageResource(iconResId)

        task.reminderTime?.let { timestamp ->
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.tvTaskReminderTime.text = "Pengingat: ${sdf.format(timestamp.toDate())}"
            holder.tvTaskReminderTime.visibility = View.VISIBLE
        } ?: run {
            holder.tvTaskReminderTime.visibility = View.GONE
        }

        if (userRole == "family") {
            holder.cbTaskDone.visibility = View.GONE
            holder.ivEditTask.visibility = View.VISIBLE
            holder.ivDeleteTask.visibility = View.VISIBLE
        } else { // elderly
            holder.cbTaskDone.visibility = View.VISIBLE
            holder.ivEditTask.visibility = View.GONE
            holder.ivDeleteTask.visibility = View.GONE
            holder.tvTaskTitle.textSize = 20f
            holder.tvTaskReminderTime.textSize = 16f
            holder.tvTaskStatus.textSize = 16f
            holder.cbTaskDone.textSize = 18f
        }

        if (task.status == "completed") {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskTitle.alpha = 0.5f
        } else {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskTitle.alpha = 1.0f
        }

        holder.cbTaskDone.isChecked = task.status == "completed"

        holder.ivEditTask.setOnClickListener { onEditClick(task) }
        holder.ivDeleteTask.setOnClickListener { onDeleteClick(task) }

        // DIUBAH: Logika Checkbox disederhanakan
        holder.cbTaskDone.setOnClickListener {
            // Jangan ubah status di sini. Cukup teruskan event klik ke Activity.
            // Activity akan memutuskan apakah status boleh diubah atau tidak.
            onItemClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}