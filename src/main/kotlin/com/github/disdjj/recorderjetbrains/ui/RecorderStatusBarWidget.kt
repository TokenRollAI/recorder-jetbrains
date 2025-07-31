package com.github.disdjj.recorderjetbrains.ui

import com.github.disdjj.recorderjetbrains.listeners.FileOperationListener
import com.github.disdjj.recorderjetbrains.listeners.TerminalCommandListener
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.util.messages.MessageBusConnection
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon

class RecorderStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private var fileOperationListener: FileOperationListener? = null
    private var terminalCommandListener: TerminalCommandListener? = null
    private var messageBusConnection: MessageBusConnection? = null
    
    companion object {
        const val WIDGET_ID = "RecorderStatusBarWidget"
    }
    
    override fun ID(): String = WIDGET_ID
    
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    
    override fun getText(): String {
        return if (recorderService.isRecording()) {
            "⏹ Recording (${recorderService.getLogCount()})"
        } else {
            "⏺ Start Recording"
        }
    }
    
    override fun getTooltipText(): String {
        return if (recorderService.isRecording()) {
            "Click to stop recording operations"
        } else {
            "Click to start recording operations"
        }
    }
    
    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { _ ->
            toggleRecording()
        }
    }

    override fun getAlignment(): Float = 0.0f
    
    private fun toggleRecording() {
        ApplicationManager.getApplication().invokeLater {
            if (recorderService.isRecording()) {
                stopRecording()
            } else {
                startRecording()
            }
            updateWidget()
        }
    }
    
    private fun startRecording() {
        logger.info("Starting recording...")
        
        try {
            // Start the recorder service
            recorderService.startRecording()
            
            // Setup file operation listener
            fileOperationListener = FileOperationListener(project)
            VirtualFileManager.getInstance().addVirtualFileListener(fileOperationListener!!)

            // Also register as document listener for file saves
            messageBusConnection = project.messageBus.connect()
            messageBusConnection?.subscribe(FileDocumentManagerListener.TOPIC, fileOperationListener!!)

            // Setup terminal command listener
            terminalCommandListener = TerminalCommandListener(project)
            terminalCommandListener?.attachToTerminals()
            
            logger.info("Recording started successfully")
            
        } catch (e: Exception) {
            logger.error("Error starting recording", e)
        }
    }
    
    private fun stopRecording() {
        logger.info("Stopping recording...")
        
        try {
            // Remove file operation listener
            fileOperationListener?.let { listener ->
                VirtualFileManager.getInstance().removeVirtualFileListener(listener)
            }
            fileOperationListener = null

            // Disconnect message bus
            messageBusConnection?.disconnect()
            messageBusConnection = null
            
            // Remove terminal command listener
            terminalCommandListener?.detachFromTerminals()
            terminalCommandListener = null
            
            // Stop the recorder service and save log
            val saved = recorderService.stopRecording()
            
            if (saved) {
                logger.info("Recording stopped and saved successfully")
            } else {
                logger.info("Recording stopped - no operations recorded")
            }
            
        } catch (e: Exception) {
            logger.error("Error stopping recording", e)
        }
    }
    
    private fun updateWidget() {
        ApplicationManager.getApplication().invokeLater {
            myStatusBar?.updateWidget(ID())
        }
    }
    
    override fun dispose() {
        if (recorderService.isRecording()) {
            stopRecording()
        }
        super.dispose()
    }
}
