package com.example.smartcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ElderlyUserAdapter(
    private var elderlyList: List<User>,
    private val onItemClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<ElderlyUserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // DIUBAH: ID disesuaikan dengan yang ada di item_elderly_user.xml
        val nameTextView: TextView = itemView.findViewById(R.id.tv_elderly_user_name)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_elderly)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_elderly_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = elderlyList[position]

        // DIUBAH: Menggunakan nameTextView dan memprioritaskan nama
        holder.nameTextView.text = if (user.name.isNotEmpty()) user.name else user.email

        holder.itemView.setOnClickListener {
            onItemClick(user)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(user)
        }
    }

    override fun getItemCount(): Int = elderlyList.size

    fun updateUsers(newUsers: List<User>) {
        elderlyList = newUsers
        notifyDataSetChanged()
    }
}