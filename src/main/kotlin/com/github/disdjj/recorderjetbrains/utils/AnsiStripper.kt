package com.github.disdjj.recorderjetbrains.utils

/**
 * Utility class for stripping ANSI escape codes from terminal output
 */
object AnsiStripper {
    
    /**
     * Regex pattern to match ANSI escape codes
     * This matches various ANSI escape sequences including:
     * - Color codes
     * - Cursor movement
     * - Text formatting
     * - Screen clearing
     */
    private val ANSI_ESCAPE_REGEX = Regex("\\u001b\\[[0-9;]*[a-zA-Z]")
    
    /**
     * Strips ANSI escape codes from the given text
     * 
     * @param text The text containing ANSI escape codes
     * @return The text with ANSI escape codes removed
     */
    fun stripAnsi(text: String): String {
        return text.replace(ANSI_ESCAPE_REGEX, "")
    }
    
    /**
     * Checks if the given text contains ANSI escape codes
     * 
     * @param text The text to check
     * @return true if the text contains ANSI escape codes, false otherwise
     */
    fun containsAnsi(text: String): Boolean {
        return ANSI_ESCAPE_REGEX.containsMatchIn(text)
    }
}
