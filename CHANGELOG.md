<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# recorder Changelog

## [Unreleased]

## [0.1.0] - 2025-01-31
### Added
- 📁 File operations recording (create, delete, modify)
- 🖥️ Terminal command recording via shell history monitoring
- 📊 Git integration with diff recording for tracked files
- 🎯 Smart .gitignore pattern filtering
- 💾 JSON export functionality for recorded operations
- 🔄 Real-time status bar widget with operation count
- 🧹 ANSI escape code stripping for clean terminal output
- ⚙️ Toggle recording via status bar widget and Tools menu
- 🧪 Comprehensive test coverage for core functionality

### Technical Details
- Multi-shell support: bash, zsh, fish, PowerShell
- Cross-platform compatibility (Windows, macOS, Linux)
- Thread-safe implementation using IntelliJ platform APIs
- Proper resource management with Disposable pattern
- Security-conscious design avoiding command re-execution
