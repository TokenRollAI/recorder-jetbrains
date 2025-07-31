package com.github.disdjj.recorderjetbrains.services

import com.github.disdjj.recorderjetbrains.model.LogEntry
import com.github.disdjj.recorderjetbrains.model.LogType
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class RecorderService(private val project: Project) {

    private val logger = thisLogger()
    private val operationLog = CopyOnWriteArrayList<LogEntry>()
    private var isRecording = false

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    fun isRecording(): Boolean = isRecording

    fun getLogCount(): Int = operationLog.size
    
    fun startRecording() {
        logger.info("Starting recording for project: ${project.name}")
        isRecording = true
        operationLog.clear()
    }
    
    fun stopRecording(): Boolean {
        logger.info("Stopping recording for project: ${project.name}")
        isRecording = false
        
        if (operationLog.isEmpty()) {
            logger.info("No operations recorded")
            return false
        }
        
        return saveOperationLog()
    }
    
    fun addLogEntry(entry: LogEntry) {
        if (isRecording) {
            logger.info("Adding log entry: ${entry.type} - ${entry.path}")
            operationLog.add(entry)
        } else {
            logger.debug("Not recording, ignoring log entry: ${entry.type} - ${entry.path}")
        }
    }
    
    fun addCommandEntry(command: String, output: String) {
        addLogEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogType.COMMAND,
            command = command,
            output = stripAnsiCodes(output)
        ))
    }
    
    fun addFileCreateEntry(path: String) {
        addLogEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogType.FILE_CREATE,
            path = path,
            data = ""
        ))
    }
    
    fun addFileDeleteEntry(path: String) {
        addLogEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogType.FILE_DELETE,
            path = path,
            data = ""
        ))
    }
    
    fun addFileDiffEntry(path: String, diff: String) {
        addLogEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogType.FILE_DIFF,
            path = path,
            data = diff
        ))
    }
    
    fun addFileContentEntry(path: String, content: String) {
        addLogEntry(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogType.FILE_CONTENT,
            path = path,
            data = content
        ))
    }
    
    private fun saveOperationLog(): Boolean {
        return try {
            val projectBasePath = project.basePath ?: return false
            val logFile = File(projectBasePath, "operation.json")
            val jsonContent = gson.toJson(operationLog)
            logFile.writeText(jsonContent)

            val entryCount = operationLog.size
            logger.info("Operation log saved to: ${logFile.absolutePath} with $entryCount entries")

            true
        } catch (e: Exception) {
            logger.error("Failed to save operation log", e)
            false
        }
    }

    // Notification functionality removed for simplicity
    
    private fun stripAnsiCodes(text: String): String {
        // Regex to strip ANSI escape codes
        val ansiRegex = "\\u001b\\[[0-9;]*[a-zA-Z]".toRegex()
        return text.replace(ansiRegex, "")
    }
}
