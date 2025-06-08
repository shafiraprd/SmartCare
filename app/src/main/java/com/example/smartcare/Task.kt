package com.example.smartcare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Task(
    @DocumentId
    val id: String = "",

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("createdBy")
    @set:PropertyName("createdBy")
    var createdBy: String = "",

    @get:PropertyName("assignedTo")
    @set:PropertyName("assignedTo")
    var assignedTo: String = "",

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now(),

    // ▼▼▼ FIELD BARU UNTUK WAKTU PENGINGAT ▼▼▼
    @get:PropertyName("reminderTime")
    @set:PropertyName("reminderTime")
    var reminderTime: Timestamp? = null // Nullable, karena tidak semua tugas punya pengingat
)