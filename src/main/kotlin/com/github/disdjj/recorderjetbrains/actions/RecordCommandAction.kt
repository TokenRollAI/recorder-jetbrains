package com.github.disdjj.recorderjetbrains.actions

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class RecordCommandAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val recorderService = project.service<RecorderService>()
        
        if (!recorderService.isRecording()) {
            Messages.showInfoMessage(
                project,
                "Please start recording first using the status bar button or Tools → Toggle Recording",
                "Recorder"
            )
            return
        }
        
        val command = Messages.showInputDialog(
            project,
            "Enter the command you executed in terminal:",
            "Record Terminal Command",
            Messages.getQuestionIcon(),
            "",
            null
        )
        
        if (!command.isNullOrBlank()) {
            recorderService.addCommandEntry(command.trim(), "")
            Messages.showInfoMessage(
                project,
                "Command recorded: $command",
                "Recorder"
            )
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        
        val recorderService = project.service<RecorderService>()
        e.presentation.isEnabled = recorderService.isRecording()
        e.presentation.text = "Record Terminal Command"
    }
}
