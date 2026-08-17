package com.example.filemanager.download

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val filename: String,
    val destUri: String,
    val protocol: String,
    val credentialPrefix: String? = null,
    val privateKeyName: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: Int = Status.QUEUED,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    object Status {
        const val QUEUED = 0
        const val RUNNING = 1
        const val PAUSED = 2
        const val COMPLETED = 3
        const val FAILED = 4
        const val CANCELED = 5
    }
}
