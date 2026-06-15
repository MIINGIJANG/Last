package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Long = 1,
    val userId: String,
    val userName: String,
    val email: String = "",
)
