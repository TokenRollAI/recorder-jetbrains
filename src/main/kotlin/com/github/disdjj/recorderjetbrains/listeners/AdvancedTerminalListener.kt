package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced terminal listener that uses multiple approaches to capture terminal commands
 */
class AdvancedTerminalListener(private val project: Project) {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private var messageBusConnection: MessageBusConnection? = null
    private val executionEnvironments = ConcurrentHashMap<ProcessHandler, ExecutionEnvironment>()
    
    fun startListening() {
        try {
            logger.info("Starting advanced terminal listening...")
            
            // Listen to execution events
            messageBusConnection = project.messageBus.connect()
            messageBusConnection?.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
                override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
                    if (!recorderService.isRecording()) return
                    
                    logger.debug("Process start scheduled: $executorId")
                    // This might be a terminal command or run configuration
                }
                
                override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                    if (!recorderService.isRecording()) return

                    logger.debug("Process started: $executorId")
                    executionEnvironments[handler] = env

                    // Try to extract command information
                    val commandLine = extractCommandFromEnvironment(env)
                    if (commandLine != null) {
                        logger.info("Detected command execution: $commandLine")
                        ApplicationManager.getApplication().invokeLater {
                            recorderService.addCommandEntry(commandLine, "")
                        }
                    }

                    // Also try to monitor process output
                    monitorProcessOutput(handler)
                }
                
                override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
                    if (!recorderService.isRecording()) return
                    
                    logger.debug("Process terminated: $executorId with exit code: $exitCode")
                    executionEnvironments.remove(handler)
                }
            })
            
            logger.info("Advanced terminal listening started")
        } catch (e: Exception) {
            logger.error("Error starting advanced terminal listening", e)
        }
    }
    
    private fun extractCommandFromEnvironment(env: ExecutionEnvironment): String? {
        try {
            val runProfile = env.runProfile
            val configurationName = runProfile.name
            
            // Check if this is a terminal-related execution
            if (configurationName.contains("Terminal", ignoreCase = true) ||
                configurationName.contains("Shell", ignoreCase = true) ||
                configurationName.contains("Command", ignoreCase = true)) {
                
                logger.debug("Detected terminal-related execution: $configurationName")
                return configurationName
            }
            
            // Try to get command line from run configuration
            val commandLine = runProfile.toString()
            if (commandLine.isNotEmpty() && commandLine != configurationName) {
                return commandLine
            }
            
            return null
        } catch (e: Exception) {
            logger.debug("Error extracting command from environment: ${e.message}")
            return null
        }
    }
    
    fun stopListening() {
        try {
            logger.info("Stopping advanced terminal listening...")
            
            messageBusConnection?.disconnect()
            messageBusConnection = null
            executionEnvironments.clear()
            
            logger.info("Advanced terminal listening stopped")
        } catch (e: Exception) {
            logger.error("Error stopping advanced terminal listening", e)
        }
    }

    private fun monitorProcessOutput(handler: ProcessHandler) {
        try {
            handler.addProcessListener(object : com.intellij.execution.process.ProcessAdapter() {
                private val commandBuffer = StringBuilder()

                override fun onTextAvailable(event: com.intellij.execution.process.ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                    if (!recorderService.isRecording()) return

                    val text = event.text ?: return

                    // Look for command patterns in the output
                    if (text.contains("$") || text.contains("#") || text.contains(">")) {
                        // This might be a command prompt
                        val lines = text.split("\n")
                        for (line in lines) {
                            val cleanLine = line.trim()
                            if (cleanLine.isNotEmpty() && isLikelyCommand(cleanLine)) {
                                logger.info("Detected terminal command from output: $cleanLine")
                                ApplicationManager.getApplication().invokeLater {
                                    recorderService.addCommandEntry(cleanLine, "")
                                }
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            logger.debug("Error monitoring process output: ${e.message}")
        }
    }

    private fun isLikelyCommand(line: String): Boolean {
        // Simple heuristics to detect if a line looks like a command
        return line.length > 2 &&
               !line.startsWith("Welcome") &&
               !line.startsWith("Last login") &&
               !line.contains("@") && // Avoid username@hostname
               line.any { it.isLetterOrDigit() }
    }
}
