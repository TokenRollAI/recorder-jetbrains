package com.github.disdjj.recorderjetbrains.actions

import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import java.io.File

class TestRecorderAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val recorderService = project.service<RecorderService>()
        
        try {
            // Test basic functionality
            recorderService.startRecording()
            
            // Add some test entries
            recorderService.addCommandEntry("ls -la", "")
            recorderService.addFileCreateEntry("test.txt")
            recorderService.addFileContentEntry("test.txt", "Hello World!")
            
            val saved = recorderService.stopRecording()
            
            if (saved) {
                val projectPath = project.basePath ?: "unknown"
                val logFile = File(projectPath, "operation.json")
                
                Messages.showInfoMessage(
                    project,
                    "Test completed successfully!\n" +
                    "Log saved to: ${logFile.absolutePath}\n" +
                    "Entries recorded: ${recorderService.getLogCount()}",
                    "Recorder Test"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "Test failed - could not save log file",
                    "Recorder Test"
                )
            }

        } catch (e: Exception) {
            Messages.showInfoMessage(
                project,
                "Test failed with error: ${e.message}",
                "Recorder Test"
            )
        }
    }
}
