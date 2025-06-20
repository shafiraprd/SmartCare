package com.example.smartcare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    var id: String = "",
    var title: String = "",
    var createdBy: String = "",
    var assignedTo: String = "",
    @ServerTimestamp
    var createdAt: Timestamp? = null,
    var reminderTime: Timestamp? = null,
    var status: String = "pending",
    // BARU: Tambahkan properti untuk aturan pengulangan
    // Nilai bisa "none", "daily"
    var recurrence: String = "none"
)