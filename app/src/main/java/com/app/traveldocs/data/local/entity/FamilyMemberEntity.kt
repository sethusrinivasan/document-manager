package com.app.traveldocs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pinHash: String,
    val pinSalt: String,
    val createdAt: Long,
    val failedAttempts: Int = 0,
    val lockedUntil: Long? = null
)
