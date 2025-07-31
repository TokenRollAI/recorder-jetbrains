package com.github.disdjj.recorderjetbrains.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a log entry for recording operations
 */
data class LogEntry(
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("type")
    val type: LogType,
    
    @SerializedName("path")
    val path: String? = null,
    
    @SerializedName("command")
    val command: String? = null,
    
    @SerializedName("output")
    val output: String? = null,
    
    @SerializedName("data")
    val data: String? = null
)

enum class LogType {
    @SerializedName("COMMAND")
    COMMAND,
    
    @SerializedName("FILE_CREATE")
    FILE_CREATE,
    
    @SerializedName("FILE_DELETE")
    FILE_DELETE,
    
    @SerializedName("FILE_DIFF")
    FILE_DIFF,
    
    @SerializedName("FILE_CONTENT")
    FILE_CONTENT
}
