package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Safe terminal listener that minimizes interference with terminal internals
 */
class SafeTerminalListener(private val project: Project) : AWTEventListener {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val commandBuffer = StringBuilder()
    private var isListening = false
    private var lastTerminalCheck = 0L
    private var isInTerminalCache = false
    
    // Cache to avoid frequent terminal checks
    private val terminalCheckInterval = 1000L // 1 second
    
    fun startListening() {
        try {
            if (!isListening) {
                logger.info("Starting safe terminal command listening...")
                
                // Register as AWT event listener for key events only
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)
                isListening = true
                
                logger.info("Safe terminal command listening started")
            }
        } catch (e: Exception) {
            logger.error("Error starting safe terminal listening", e)
        }
    }
    
    fun stopListening() {
        try {
            if (isListening) {
                logger.info("Stopping safe terminal command listening...")
                
                Toolkit.getDefaultToolkit().removeAWTEventListener(this)
                isListening = false
                commandBuffer.clear()
                
                logger.info("Safe terminal command listening stopped")
            }
        } catch (e: Exception) {
            logger.error("Error stopping safe terminal listening", e)
        }
    }
    
    override fun eventDispatched(event: AWTEvent) {
        if (!recorderService.isRecording() || event !is KeyEvent) return
        
        try {
            // Only process KEY_TYPED events to avoid duplicates
            if (event.id != KeyEvent.KEY_TYPED) return
            
            // Check if we're in terminal (with caching to reduce overhead)
            if (!isInTerminal()) return
            
            handleKeyTyped(event)
            
        } catch (e: Exception) {
            // Silently ignore errors to avoid interfering with terminal
            logger.debug("Error processing key event: ${e.message}")
        }
    }
    
    private fun isInTerminal(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Use cached result if recent
        if (currentTime - lastTerminalCheck < terminalCheckInterval) {
            return isInTerminalCache
        }
        
        try {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")
            
            isInTerminalCache = terminalToolWindow?.isVisible == true && 
                               terminalToolWindow.isActive
            
            lastTerminalCheck = currentTime
            return isInTerminalCache
            
        } catch (e: Exception) {
            logger.debug("Error checking terminal state: ${e.message}")
            isInTerminalCache = false
            lastTerminalCheck = currentTime
            return false
        }
    }
    
    private fun handleKeyTyped(event: KeyEvent) {
        val char = event.keyChar
        
        when (char) {
            '\n', '\r' -> {
                // Enter pressed - command executed
                val command = commandBuffer.toString().trim()
                if (command.isNotEmpty() && isValidCommand(command)) {
                    logger.info("Terminal command detected: $command")
                    
                    // Record command asynchronously to avoid blocking
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            recorderService.addCommandEntry(command, "")
                        } catch (e: Exception) {
                            logger.debug("Error recording command: ${e.message}")
                        }
                    }
                }
                commandBuffer.clear()
            }
            '\b' -> {
                // Backspace
                if (commandBuffer.isNotEmpty()) {
                    commandBuffer.deleteCharAt(commandBuffer.length - 1)
                }
            }
            else -> {
                // Regular character - be more selective about what we capture
                if (isValidCommandChar(char)) {
                    commandBuffer.append(char)
                    
                    // Limit buffer size to prevent memory issues
                    if (commandBuffer.length > 1000) {
                        commandBuffer.clear()
                    }
                }
            }
        }
    }
    
    private fun isValidCommand(command: String): Boolean {
        // Basic validation to filter out noise
        return command.length >= 2 && 
               command.any { it.isLetterOrDigit() } &&
               !command.startsWith("^") && // Avoid control sequences
               !command.all { it.isWhitespace() }
    }
    
    private fun isValidCommandChar(char: Char): Boolean {
        // More restrictive character set to avoid capturing terminal control sequences
        return char.isLetterOrDigit() || 
               char in " .-_/\\:@#$%^&*()+=[]{}|;'\",./<>?`~"
    }
}
