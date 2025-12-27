package com.example.vault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaFile(
    @PrimaryKey val id: String,
    val originalName: String,
    val mimeType: String,
    val size: Long,
    val importTime: Long,
    val encryptedPath: String,
    val iv: ByteArray,
    val thumbPath: String?,
    val viewedDate: Long? = null
)