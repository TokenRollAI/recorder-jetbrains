package com.github.disdjj.recorderjetbrains.actions

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class ToggleRecordingAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val recorderService = project.service<RecorderService>()
        
        if (recorderService.isRecording()) {
            val saved = recorderService.stopRecording()
            if (saved) {
                Messages.showInfoMessage(
                    project,
                    "Recording stopped and saved to operation.json",
                    "Recorder"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "Recording stopped - no operations recorded",
                    "Recorder"
                )
            }
        } else {
            recorderService.startRecording()
            Messages.showInfoMessage(
                project,
                "Recording started",
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
        e.presentation.text = if (recorderService.isRecording()) {
            "Stop Recording"
        } else {
            "Start Recording"
        }
    }
}
