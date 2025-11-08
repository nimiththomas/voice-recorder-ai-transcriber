package com.nimith.echonote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Transcript(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String
)