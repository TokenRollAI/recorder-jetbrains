# Terminal Recording Functionality

This document describes the terminal command recording functionality added to the JetBrains Recorder plugin.

## Overview

The terminal recording feature captures terminal commands executed during a recording session and includes them in the operation log alongside file operations.

## Implementation

### Components Added

1. **TerminalCommandListener** (`src/main/kotlin/com/github/disdjj/recorderjetbrains/listeners/TerminalCommandListener.kt`)
   - Monitors shell history files for new commands
   - Supports multiple shells: bash, zsh, fish, PowerShell
   - Records commands with timestamps

2. **AnsiStripper** (`src/main/kotlin/com/github/disdjj/recorderjetbrains/utils/AnsiStripper.kt`)
   - Utility to remove ANSI escape codes from terminal output
   - Cleans up terminal text for better readability in logs

3. **Enhanced LogEntry Model**
   - Added `command` and `output` fields to support terminal commands
   - Maintains backward compatibility with existing log types

4. **Updated RecorderService**
   - Added `addCommandEntry()` method for recording terminal commands
   - Integrates seamlessly with existing recording functionality

### How It Works

1. **History File Monitoring**: The listener monitors shell history files:
   - Linux/macOS: `~/.bash_history`, `~/.zsh_history`, `~/.local/share/fish/fish_history`
   - Windows: `%APPDATA%/Microsoft/Windows/PowerShell/PSReadLine/ConsoleHost_history.txt`

2. **Command Detection**: Periodically checks for new entries in history files

3. **Recording**: When new commands are detected during a recording session:
   - Commands are cleaned of shell-specific formatting
   - Logged with timestamps as `COMMAND` type entries
   - Included in the final `operation.json` file

## Usage

### Starting Terminal Recording

1. **Via Status Bar Widget**: Click the recording widget in the status bar
2. **Via Menu**: Tools → Toggle Recording
3. **Via Test Action**: Tools → Test Terminal Recording (for testing)

### Recording Process

1. Start recording using any of the above methods
2. Open a terminal in your IDE or system
3. Execute commands as normal
4. Commands will be automatically detected and recorded
5. Stop recording to save the log

### Log Format

Terminal commands appear in the `operation.json` file as:

```json
{
  "timestamp": 1640995200000,
  "type": "COMMAND",
  "command": "ls -la",
  "output": ""
}
```

## Limitations

1. **History-Based Detection**: Commands are detected from shell history files, which may have limitations:
   - Some shells don't immediately write to history
   - Private/incognito sessions may not be recorded
   - Commands that don't get added to history (e.g., those starting with space in bash) won't be captured

2. **Output Capture**: Currently, command output is not captured to avoid security risks and complexity

3. **Shell Support**: Limited to common shells with accessible history files

## Security Considerations

- Commands are recorded as they appear in shell history
- No command output is captured to prevent sensitive data exposure
- No commands are re-executed during the recording process

## Future Improvements

1. **Real-time Terminal Integration**: Direct integration with IDE terminal widgets
2. **Output Capture**: Safe capture of command output when appropriate
3. **Command Filtering**: Options to exclude certain commands or patterns
4. **Enhanced Shell Support**: Support for more shell types and configurations

## Testing

Use the "Test Terminal Recording" action in the Tools menu to verify the functionality is working correctly in your environment.

## Dependencies

- Requires `org.jetbrains.plugins.terminal` plugin dependency
- Compatible with IntelliJ Platform 2024.3+
