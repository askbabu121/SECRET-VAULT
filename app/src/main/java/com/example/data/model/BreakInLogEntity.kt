package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "break_in_logs")
data class BreakInLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptType: String = "WRONG_PIN",
    val enteredCodeMasked: String = "****"
)
