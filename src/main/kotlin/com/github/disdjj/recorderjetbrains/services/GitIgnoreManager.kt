package com.github.disdjj.recorderjetbrains.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.regex.Pattern

class GitIgnoreManager(private val project: Project) {
    
    private val logger = thisLogger()
    private var ignorePatterns: List<Pattern> = emptyList()
    
    init {
        loadGitIgnore()
    }
    
    fun loadGitIgnore() {
        val projectBasePath = project.basePath ?: return
        val gitIgnoreFile = File(projectBasePath, ".gitignore")
        
        if (!gitIgnoreFile.exists()) {
            logger.info("No .gitignore file found")
            ignorePatterns = emptyList()
            return
        }
        
        try {
            val lines = gitIgnoreFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
            
            ignorePatterns = lines.map { pattern ->
                convertGitIgnorePatternToRegex(pattern)
            }
            
            logger.info("Loaded ${ignorePatterns.size} ignore patterns from .gitignore")
        } catch (e: Exception) {
            logger.error("Failed to load .gitignore", e)
            ignorePatterns = emptyList()
        }
    }
    
    fun isIgnored(relativePath: String): Boolean {
        // Always ignore .git directory
        if (relativePath.startsWith(".git/") || relativePath == ".git") {
            return true
        }
        
        return ignorePatterns.any { pattern ->
            pattern.matcher(relativePath).matches()
        }
    }
    
    private fun convertGitIgnorePatternToRegex(gitPattern: String): Pattern {
        var pattern = gitPattern
        
        // Handle directory patterns (ending with /)
        val isDirectory = pattern.endsWith("/")
        if (isDirectory) {
            pattern = pattern.dropLast(1)
        }
        
        // Escape special regex characters except * and ?
        pattern = pattern.replace(".", "\\.")
            .replace("+", "\\+")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("|", "\\|")
        
        // Convert gitignore wildcards to regex
        pattern = pattern.replace("*", ".*")
            .replace("?", ".")
        
        // Handle leading slash (absolute path from repo root)
        if (pattern.startsWith("/")) {
            pattern = pattern.substring(1)
        } else {
            // Pattern can match anywhere in the path
            pattern = "(.*/)?" + pattern
        }
        
        // Handle directory patterns
        if (isDirectory) {
            pattern = "$pattern(/.*)?$"
        } else {
            pattern = "$pattern$"
        }
        
        return Pattern.compile(pattern)
    }
}
