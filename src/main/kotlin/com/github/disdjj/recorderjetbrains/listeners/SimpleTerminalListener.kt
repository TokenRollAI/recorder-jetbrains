package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Simple terminal listener that captures commands using keyboard monitoring
 */
class SimpleTerminalListener(private val project: Project) : AWTEventListener {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val commandBuffer = StringBuilder()
    private var isInTerminal = false
    private var isListening = false
    
    fun startListening() {
        try {
            if (!isListening) {
                logger.info("Starting simple terminal command listening...")
                
                // Register as AWT event listener for key events
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)
                isListening = true
                
                logger.info("Simple terminal command listening started")
            }
        } catch (e: Exception) {
            logger.error("Error starting simple terminal listening", e)
        }
    }
    
    fun stopListening() {
        try {
            if (isListening) {
                logger.info("Stopping simple terminal command listening...")
                
                Toolkit.getDefaultToolkit().removeAWTEventListener(this)
                isListening = false
                commandBuffer.clear()
                
                logger.info("Simple terminal command listening stopped")
            }
        } catch (e: Exception) {
            logger.error("Error stopping simple terminal listening", e)
        }
    }
    
    override fun eventDispatched(event: AWTEvent) {
        if (!recorderService.isRecording() || event !is KeyEvent) return
        
        try {
            // Check if we're in a terminal window
            val component = event.component
            isInTerminal = isTerminalComponent(component)
            
            if (!isInTerminal) return
            
            when (event.id) {
                KeyEvent.KEY_TYPED -> {
                    handleKeyTyped(event)
                }
                KeyEvent.KEY_PRESSED -> {
                    handleKeyPressed(event)
                }
            }
        } catch (e: Exception) {
            logger.debug("Error processing key event: ${e.message}")
        }
    }
    
    private fun handleKeyTyped(event: KeyEvent) {
        val char = event.keyChar
        
        when (char) {
            '\n', '\r' -> {
                // Enter pressed - command executed
                val command = commandBuffer.toString().trim()
                if (command.isNotEmpty()) {
                    logger.info("Terminal command detected: $command")
                    
                    ApplicationManager.getApplication().invokeLater {
                        recorderService.addCommandEntry(command, "")
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
                // Regular character
                if (char.isLetterOrDigit() || char in " !@#$%^&*()_+-=[]{}|;':\",./<>?`~\\") {
                    commandBuffer.append(char)
                }
            }
        }
    }
    
    private fun handleKeyPressed(event: KeyEvent) {
        when (event.keyCode) {
            KeyEvent.VK_C -> {
                if (event.isControlDown) {
                    // Ctrl+C - clear command buffer
                    commandBuffer.clear()
                }
            }
            KeyEvent.VK_U -> {
                if (event.isControlDown) {
                    // Ctrl+U - clear line
                    commandBuffer.clear()
                }
            }
            KeyEvent.VK_L -> {
                if (event.isControlDown) {
                    // Ctrl+L - clear screen (but not command)
                    // Don't clear buffer
                }
            }
        }
    }
    
    private fun isTerminalComponent(component: Component?): Boolean {
        if (component == null) return false

        try {
            // More conservative approach - only check class names to avoid interfering with terminal internals
            val componentClass = component.javaClass.name.lowercase()

            // Check for terminal-related class names
            val isTerminalClass = componentClass.contains("terminal") ||
                                 componentClass.contains("shell") ||
                                 componentClass.contains("console") ||
                                 componentClass.contains("jediterm")

            if (isTerminalClass) {
                logger.debug("Detected terminal component: ${component.javaClass.simpleName}")
                return true
            }

            // Secondary check: verify if we're in Terminal tool window (safer approach)
            return isInTerminalToolWindow(component)

        } catch (e: Exception) {
            logger.debug("Error checking if component is terminal: ${e.message}")
            return false
        }
    }

    private fun isInTerminalToolWindow(component: Component): Boolean {
        try {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")

            return terminalToolWindow != null &&
                   terminalToolWindow.isVisible &&
                   isDescendantOf(component, terminalToolWindow.component)
        } catch (e: Exception) {
            logger.debug("Error checking terminal tool window: ${e.message}")
            return false
        }
    }
    
    private fun isDescendantOf(component: Component, ancestor: Component): Boolean {
        var parent = component.parent
        while (parent != null) {
            if (parent == ancestor) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
}
