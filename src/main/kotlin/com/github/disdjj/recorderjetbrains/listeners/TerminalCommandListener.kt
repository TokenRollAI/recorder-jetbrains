package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.model.LogEntry
import com.github.disdjj.recorderjetbrains.model.LogType
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.github.disdjj.recorderjetbrains.utils.AnsiStripper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Listener for terminal command execution events
 * This implementation uses a simplified approach to monitor shell history
 */
class TerminalCommandListener(private val project: Project) {

    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var isMonitoring = false
    private var lastHistorySize = 0
    
    /**
     * Start monitoring terminal commands by checking shell history
     */
    fun startMonitoring() {
        logger.info("Starting terminal command monitoring via shell history")
        isMonitoring = true

        try {
            // Initialize history size
            lastHistorySize = getCurrentHistorySize()

            // Start periodic monitoring
            executor.scheduleWithFixedDelay({
                if (isMonitoring) {
                    checkForNewCommands()
                }
            }, 2, 3, TimeUnit.SECONDS)

        } catch (e: Exception) {
            logger.error("Error starting terminal monitoring", e)
        }
    }
    
    /**
     * Stop monitoring terminal commands
     */
    fun stopMonitoring() {
        logger.info("Stopping terminal command monitoring")
        isMonitoring = false

        try {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: Exception) {
            logger.error("Error stopping terminal monitoring", e)
        }
    }
    
    private fun checkForNewCommands() {
        try {
            val currentHistorySize = getCurrentHistorySize()
            if (currentHistorySize > lastHistorySize) {
                val newCommands = getNewHistoryCommands(lastHistorySize, currentHistorySize)
                newCommands.forEach { command ->
                    recordCommand(command)
                }
                lastHistorySize = currentHistorySize
            }
        } catch (e: Exception) {
            logger.debug("Error checking for new commands", e)
        }
    }

    private fun getCurrentHistorySize(): Int {
        return try {
            val historyFile = getHistoryFile()
            if (historyFile?.exists() == true) {
                historyFile.readLines().size
            } else {
                0
            }
        } catch (e: Exception) {
            logger.debug("Error getting history size", e)
            0
        }
    }

    private fun getHistoryFile(): File? {
        return try {
            val userHome = System.getProperty("user.home")
            val shell = System.getenv("SHELL") ?: ""

            when {
                shell.contains("bash") -> File(userHome, ".bash_history")
                shell.contains("zsh") -> File(userHome, ".zsh_history")
                shell.contains("fish") -> File(userHome, ".local/share/fish/fish_history")
                System.getProperty("os.name").lowercase().contains("windows") -> {
                    // For PowerShell on Windows
                    File(System.getenv("APPDATA") ?: userHome, "Microsoft/Windows/PowerShell/PSReadLine/ConsoleHost_history.txt")
                }
                else -> File(userHome, ".bash_history") // Default fallback
            }
        } catch (e: Exception) {
            logger.debug("Error determining history file", e)
            null
        }
    }
    
    private fun getNewHistoryCommands(fromIndex: Int, toIndex: Int): List<String> {
        return try {
            val historyFile = getHistoryFile()
            if (historyFile?.exists() == true) {
                val allLines = historyFile.readLines()
                if (fromIndex < allLines.size) {
                    allLines.subList(fromIndex, minOf(toIndex, allLines.size))
                        .map { line ->
                            // Clean up history line (remove timestamps for zsh, etc.)
                            cleanHistoryLine(line)
                        }
                        .filter { it.isNotBlank() }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.debug("Error getting new history commands", e)
            emptyList()
        }
    }

    private fun cleanHistoryLine(line: String): String {
        return try {
            // Handle zsh history format: ": timestamp:duration;command"
            if (line.startsWith(":") && line.contains(";")) {
                line.substringAfter(";")
            } else {
                line
            }
        } catch (e: Exception) {
            line
        }
    }

    private fun recordCommand(command: String) {
        try {
            ApplicationManager.getApplication().invokeLater {
                if (recorderService.isRecording()) {
                    // Execute the command to get output (simplified approach)
                    executeCommandForOutput(command) { output ->
                        val logEntry = LogEntry(
                            timestamp = System.currentTimeMillis(),
                            type = LogType.COMMAND,
                            command = command,
                            output = output
                        )
                        recorderService.addLogEntry(logEntry)
                        logger.debug("Recorded terminal command: $command")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error recording terminal command", e)
        }
    }

    private fun executeCommandForOutput(command: String, callback: (String) -> Unit) {
        try {
            // This is a simplified approach - in practice, you might want to avoid re-executing commands
            // Instead, you could try to capture output from the actual terminal session

            // For now, just record the command without output to avoid security issues
            callback("") // Empty output for safety

        } catch (e: Exception) {
            logger.debug("Error executing command for output", e)
            callback("")
        }
    }
}
