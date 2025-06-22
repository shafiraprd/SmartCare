package com.example.smartcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter untuk menampilkan daftar pengguna lansia di RecyclerView
class ElderlyUserAdapter(
    private var elderlyList: List<User>, // daftar data pengguna lansia
    private val onItemClick: (User) -> Unit, // callback saat item ditekan
    private val onDeleteClick: (User) -> Unit // callback saat tombol delete ditekan
) : RecyclerView.Adapter<ElderlyUserAdapter.UserViewHolder>() {

    // Kelas ViewHolder untuk menyimpan referensi ke komponen dalam layout item
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_elderly_user_name) // TextView untuk nama lansia
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_elderly) // Tombol delete
    }

    // Dipanggil saat RecyclerView butuh ViewHolder baru
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Mengubah layout XML menjadi objek View
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_elderly_user, parent, false)
        return UserViewHolder(view) // Balikkan ViewHolder yang sudah dibuat
    }

    // Dipanggil untuk mengisi data ke tampilan setiap item di RecyclerView
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = elderlyList[position] // ambil data user berdasarkan posisi

        // Kalau nama tidak kosong, tampilkan nama. Kalau kosong, pakai email sebagai fallback
        holder.nameTextView.text = if (user.name.isNotEmpty()) user.name else user.email

        // Atur aksi saat item diklik
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }

        // Atur aksi saat tombol delete diklik
        holder.deleteButton.setOnClickListener {
            onDeleteClick(user)
        }
    }

    // Menentukan jumlah item dalam RecyclerView (jumlah user)
    override fun getItemCount(): Int = elderlyList.size

    // Method untuk memperbarui isi daftar pengguna dan refresh tampilannya
    fun updateUsers(newUsers: List<User>) {
        elderlyList = newUsers
        notifyDataSetChanged() // beri tahu RecyclerView kalau datanya berubah
    }
}
