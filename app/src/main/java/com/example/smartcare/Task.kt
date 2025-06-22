package com.example.smartcare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    var id: String = "",
    var title: String = "",
    var notes: String = "", // Pastikan properti ini ada
    var createdBy: String = "",
    var assignedTo: String = "",
    @ServerTimestamp
    var createdAt: Timestamp? = null,
    var reminderTime: Timestamp? = null,
    var status: String = "pending",
    var recurrence: String = "none"
)