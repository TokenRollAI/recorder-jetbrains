package com.github.disdjj.recorderjetbrains.listeners

import com.github.disdjj.recorderjetbrains.services.GitDiffProvider
import com.github.disdjj.recorderjetbrains.services.GitIgnoreManager
import com.github.disdjj.recorderjetbrains.services.RecorderService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import java.io.File

class FileOperationListener(private val project: Project) : VirtualFileListener, FileDocumentManagerListener {
    
    private val logger = thisLogger()
    private val recorderService = project.service<RecorderService>()
    private val gitIgnoreManager = GitIgnoreManager(project)
    private val gitDiffProvider = GitDiffProvider(project)
    
    override fun fileCreated(event: VirtualFileEvent) {
        if (!recorderService.isRecording()) return
        
        val relativePath = getRelativePath(event.file) ?: return
        
        if (shouldIgnoreFile(relativePath)) {
            logger.debug("Ignoring file creation: $relativePath")
            return
        }
        
        logger.debug("File created: $relativePath")
        recorderService.addFileCreateEntry(relativePath)
    }
    
    override fun fileDeleted(event: VirtualFileEvent) {
        if (!recorderService.isRecording()) return
        
        val relativePath = getRelativePath(event.file) ?: return
        
        if (shouldIgnoreFile(relativePath)) {
            logger.debug("Ignoring file deletion: $relativePath")
            return
        }
        
        logger.debug("File deleted: $relativePath")
        recorderService.addFileDeleteEntry(relativePath)
    }
    
    // Handle file saves through FileDocumentManagerListener
    override fun beforeDocumentSaving(document: Document) {
        if (!recorderService.isRecording()) return

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val relativePath = getRelativePath(file) ?: return

        if (shouldIgnoreFile(relativePath)) {
            logger.debug("Ignoring file save: $relativePath")
            return
        }

        logger.debug("File saved: $relativePath")
        handleFileContentChange(file, document, relativePath)
    }
    
    private fun handleFileContentChange(file: VirtualFile, document: Document, relativePath: String) {
        // For now, always try to get git diff first, then fallback to full content
        // This avoids blocking EDT operations
        gitDiffProvider.getFileDiff(file) { diff ->
            if (diff != null && diff.isNotBlank()) {
                logger.debug("Adding git diff for file: $relativePath")
                recorderService.addFileDiffEntry(relativePath, diff)
            } else {
                // No diff found or file not tracked, save full content
                logger.debug("No git diff found, saving full content for file: $relativePath")
                val content = document.text
                recorderService.addFileContentEntry(relativePath, content)
            }
        }
    }
    
    private fun getRelativePath(file: VirtualFile): String? {
        val projectBasePath = project.basePath ?: return null
        val projectBaseFile = File(projectBasePath)
        val targetFile = File(file.path)
        
        return try {
            val relativePath = projectBaseFile.toURI().relativize(targetFile.toURI()).path
            // Remove trailing slash if present
            if (relativePath.endsWith("/")) {
                relativePath.dropLast(1)
            } else {
                relativePath
            }
        } catch (e: Exception) {
            logger.debug("Error getting relative path for file: ${file.path}", e)
            null
        }
    }
    
    private fun shouldIgnoreFile(relativePath: String): Boolean {
        return gitIgnoreManager.isIgnored(relativePath)
    }
    
    fun refreshGitIgnore() {
        gitIgnoreManager.loadGitIgnore()
    }
}
