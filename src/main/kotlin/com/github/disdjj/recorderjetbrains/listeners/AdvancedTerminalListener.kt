package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.model.LogEntry
import com.github.disdjj.recorderjetbrains.model.LogType
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.github.disdjj.recorderjetbrains.utils.AnsiStripper
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced terminal listener that tries to capture both commands and outputs
 * by monitoring process execution in the IDE
 */
class AdvancedTerminalListener(private val project: Project) {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val processListeners = ConcurrentHashMap<ProcessHandler, ProcessAdapter>()
    private var isMonitoring = false
    
    fun startMonitoring() {
        logger.info("Starting advanced terminal monitoring")
        isMonitoring = true
        
        try {
            // Monitor existing processes
            monitorExistingProcesses()
            
            // Set up listener for new processes
            setupProcessListener()
            
        } catch (e: Exception) {
            logger.error("Error starting advanced terminal monitoring", e)
        }
    }
    
    fun stopMonitoring() {
        logger.info("Stopping advanced terminal monitoring")
        isMonitoring = false
        
        try {
            // Remove all process listeners
            processListeners.forEach { (processHandler, listener) ->
                try {
                    processHandler.removeProcessListener(listener)
                } catch (e: Exception) {
                    logger.debug("Error removing process listener", e)
                }
            }
            processListeners.clear()
            
        } catch (e: Exception) {
            logger.error("Error stopping advanced terminal monitoring", e)
        }
    }
    
    private fun monitorExistingProcesses() {
        try {
            val executionManager = ExecutionManager.getInstance(project)
            val runningDescriptors = executionManager.getRunningDescriptors { true }
            
            runningDescriptors.forEach { descriptor ->
                val processHandler = descriptor.processHandler
                if (processHandler != null && !processHandler.isProcessTerminated) {
                    attachToProcess(processHandler, descriptor)
                }
            }
        } catch (e: Exception) {
            logger.debug("Error monitoring existing processes", e)
        }
    }
    
    private fun setupProcessListener() {
        // This would require access to ExecutionManager listeners
        // For now, we'll rely on periodic monitoring
        logger.debug("Process listener setup completed")
    }
    
    private fun attachToProcess(processHandler: ProcessHandler, descriptor: RunContentDescriptor) {
        if (processListeners.containsKey(processHandler)) {
            return
        }
        
        logger.debug("Attaching to process: ${descriptor.displayName}")
        
        val listener = object : ProcessAdapter() {
            private val outputBuffer = StringBuilder()
            private var currentCommand = ""
            private var commandStartTime = System.currentTimeMillis()
            
            override fun startNotified(event: ProcessEvent) {
                commandStartTime = System.currentTimeMillis()
                // Try to extract command from process
                currentCommand = extractCommandFromProcess(processHandler, descriptor)
                logger.debug("Process started: $currentCommand")
            }
            
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (!isMonitoring || !recorderService.isRecording()) return
                
                try {
                    val text = event.text
                    if (text != null) {
                        outputBuffer.append(text)
                    }
                } catch (e: Exception) {
                    logger.debug("Error processing text output", e)
                }
            }
            
            override fun processTerminated(event: ProcessEvent) {
                if (!isMonitoring || !recorderService.isRecording()) return
                
                try {
                    val output = outputBuffer.toString()
                    val cleanOutput = AnsiStripper.stripAnsi(output)
                    
                    if (currentCommand.isNotEmpty()) {
                        recordCommand(currentCommand, cleanOutput, commandStartTime)
                    }
                    
                    logger.debug("Process terminated: $currentCommand")
                } catch (e: Exception) {
                    logger.error("Error processing terminated process", e)
                } finally {
                    processListeners.remove(processHandler)
                }
            }
        }
        
        processHandler.addProcessListener(listener)
        processListeners[processHandler] = listener
    }
    
    private fun extractCommandFromProcess(processHandler: ProcessHandler, descriptor: RunContentDescriptor): String {
        return try {
            // Try to get command from descriptor
            val displayName = descriptor.displayName ?: ""
            val attachedObject = descriptor.attachedContent
            
            // Extract meaningful command information
            when {
                displayName.contains("Terminal") -> "terminal session"
                displayName.contains("Run") -> displayName.substringAfter("Run ").trim()
                displayName.contains("Debug") -> displayName.substringAfter("Debug ").trim()
                else -> displayName.ifEmpty { "unknown command" }
            }
        } catch (e: Exception) {
            logger.debug("Error extracting command from process", e)
            "unknown command"
        }
    }
    
    private fun recordCommand(command: String, output: String, timestamp: Long) {
        try {
            ApplicationManager.getApplication().invokeLater {
                if (recorderService.isRecording()) {
                    val logEntry = LogEntry(
                        timestamp = timestamp,
                        type = LogType.COMMAND,
                        command = command,
                        output = output.take(1000) // Limit output size
                    )
                    recorderService.addLogEntry(logEntry)
                    logger.debug("Recorded process command: $command")
                }
            }
        } catch (e: Exception) {
            logger.error("Error recording command", e)
        }
    }
}
