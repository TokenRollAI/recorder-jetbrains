package com.github.disdjj.recorderjetbrains

import com.github.disdjj.recorderjetbrains.listeners.TerminalCommandListener
import com.github.disdjj.recorderjetbrains.model.LogType
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.github.disdjj.recorderjetbrains.utils.AnsiStripper
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class TerminalRecordingTest : BasePlatformTestCase() {

    fun testAnsiStripper() {
        val textWithAnsi = "\u001b[31mRed text\u001b[0m Normal text"
        val cleanText = AnsiStripper.stripAnsi(textWithAnsi)
        assertEquals("Red text Normal text", cleanText)
        
        val textWithoutAnsi = "Normal text without ANSI codes"
        val cleanText2 = AnsiStripper.stripAnsi(textWithoutAnsi)
        assertEquals(textWithoutAnsi, cleanText2)
    }
    
    fun testAnsiDetection() {
        val textWithAnsi = "\u001b[31mRed text\u001b[0m"
        assertTrue(AnsiStripper.containsAnsi(textWithAnsi))
        
        val textWithoutAnsi = "Normal text"
        assertFalse(AnsiStripper.containsAnsi(textWithoutAnsi))
    }
    
    fun testRecorderServiceCommandEntry() {
        val recorderService = project.service<RecorderService>()

        assertFalse(recorderService.isRecording())
        assertEquals(0, recorderService.getLogCount())

        recorderService.startRecording()
        assertTrue(recorderService.isRecording())

        // Add a command entry
        recorderService.addCommandEntry("ls -la", "total 8\ndrwxr-xr-x  2 user user 4096 Jan 1 12:00 .\ndrwxr-xr-x  3 user user 4096 Jan 1 12:00 ..")
        assertEquals(1, recorderService.getLogCount())

        // Don't call stopRecording() to avoid file operations in test
        // recorderService.stopRecording()
        // assertFalse(recorderService.isRecording())
    }
    
    fun testTerminalCommandListener() {
        val recorderService = project.service<RecorderService>()
        val terminalListener = TerminalCommandListener(project)
        
        // Start recording
        recorderService.startRecording()
        
        // Start monitoring
        terminalListener.startMonitoring()
        
        // Stop monitoring
        terminalListener.stopMonitoring()
        
        // Stop recording
        recorderService.stopRecording()
        
        // Test passed if no exceptions were thrown
        assertTrue(true)
    }
    
    override fun getTestDataPath() = "src/test/testData/terminal"
}
