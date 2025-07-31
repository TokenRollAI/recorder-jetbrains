package com.github.disdjj.recorderjetbrains.actions

import com.github.disdjj.recorderjetbrains.listeners.TerminalCommandListener
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class TestTerminalRecordingAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val recorderService = project.service<RecorderService>()
        
        if (!recorderService.isRecording()) {
            Messages.showInfoMessage(
                project,
                "Please start recording first using the status bar widget or the Toggle Recording action.",
                "Terminal Recording Test"
            )
            return
        }
        
        // Test the terminal listener
        val terminalListener = TerminalCommandListener(project)
        terminalListener.startMonitoring()
        
        Messages.showInfoMessage(
            project,
            "Terminal recording test started. Open a terminal and run some commands to test the functionality.",
            "Terminal Recording Test"
        )
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
