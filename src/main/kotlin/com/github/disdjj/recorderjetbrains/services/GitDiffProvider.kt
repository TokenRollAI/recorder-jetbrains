package com.github.disdjj.recorderjetbrains.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepositoryManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

class GitDiffProvider(private val project: Project) {
    
    private val logger = thisLogger()
    
    fun isFileTracked(file: VirtualFile): Boolean {
        return try {
            val projectBasePath = project.basePath ?: return false

            // Use a simple heuristic: check if .git directory exists
            val gitDir = java.io.File(projectBasePath, ".git")
            if (!gitDir.exists()) return false

            // For now, assume all files in git repo are potentially tracked
            // This avoids blocking git command execution on EDT
            true
        } catch (e: Exception) {
            logger.debug("Error checking if file is tracked: ${e.message}")
            false
        }
    }
    
    fun getFileDiff(file: VirtualFile, callback: (String?) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectBasePath = project.basePath
                if (projectBasePath == null) {
                    callback(null)
                    return@executeOnPooledThread
                }

                val process = ProcessBuilder()
                    .command("git", "diff", file.path)
                    .directory(java.io.File(projectBasePath))
                    .start()

                val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.readText()
                }

                val exitCode = process.waitFor()
                val result = if (exitCode == 0 && output.isNotBlank()) {
                    output
                } else {
                    null
                }

                callback(result)
            } catch (e: Exception) {
                logger.error("Error getting git diff for file: ${file.path}", e)
                callback(null)
            }
        }
    }
    
    fun isInGitRepository(): Boolean {
        return try {
            val repositoryManager = GitRepositoryManager.getInstance(project)
            val repositories = repositoryManager.repositories
            repositories.isNotEmpty()
        } catch (e: Exception) {
            logger.debug("Error checking git repository: ${e.message}")
            false
        }
    }
    
    private fun getRelativePath(file: VirtualFile): String? {
        val projectBasePath = project.basePath ?: return null
        val projectBaseFile = java.io.File(projectBasePath)
        val targetFile = java.io.File(file.path)
        
        return try {
            projectBaseFile.toURI().relativize(targetFile.toURI()).path
        } catch (e: Exception) {
            logger.debug("Error getting relative path: ${e.message}")
            null
        }
    }
}
