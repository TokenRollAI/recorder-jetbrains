package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class TerminalCommandListener(private val project: Project) {

    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val processListeners = ConcurrentHashMap<ProcessHandler, ProcessAdapter>()
    private val commandBuffers = ConcurrentHashMap<ProcessHandler, StringBuilder>()

    // Multiple listening strategies
    private val safeTerminalListener = SafeTerminalListener(project)
    private val advancedTerminalListener = AdvancedTerminalListener(project)

    // Pattern to detect command prompts (common patterns)
    private val promptPattern = Pattern.compile(".*[$#>]\\s*$")
    private val commandPattern = Pattern.compile("^[^$#>]*[$#>]\\s*(.+)$")

    fun attachToTerminals() {
        try {
            logger.info("Starting terminal command monitoring with execution listener...")

            // Use only the advanced execution monitoring - this is safer
            advancedTerminalListener.startListening()

            logger.info("Terminal command monitoring initialized")
        } catch (e: Exception) {
            logger.error("Error attaching to terminals", e)
        }
    }

    // Simplified implementation - focus on the working strategies

    // Terminal command monitoring is now handled by SimpleTerminalListener and AdvancedTerminalListener

    fun detachFromTerminals() {
        try {
            logger.info("Stopping terminal command monitoring...")
            advancedTerminalListener.stopListening()
            logger.info("Terminal command monitoring stopped")
        } catch (e: Exception) {
            logger.error("Error detaching from terminals", e)
        }
    }
}
