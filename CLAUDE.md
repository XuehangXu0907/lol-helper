# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LOL Helper is a JavaFX-based desktop application that provides automated features for League of Legends, including auto-accept functionality and a champion selector.

## Build Commands

```bash
# Clean and compile the project
mvn clean compile

# Run the application
mvn exec:java

# Run with JavaFX plugin (recommended)
mvn javafx:run

# Run tests
mvn test

# Package the application (creates shaded JAR)
mvn clean package -DskipTests

# Run a single test class
mvn test -Dtest=YourTestClassName

# Run a single test method
mvn test -Dtest=YourTestClassName#yourTestMethod

# Create Windows MSI installer
build-installer.bat

# Quick build JAR only
quick-build.bat
```

## Architecture Overview

The application follows a modular MVC architecture:

### Core Components
- **LCU Integration**: Uses OkHttp to communicate with League Client Update API for game state monitoring
- **Champion Management**: Handles champion data, avatars, and skills through dedicated managers
- **Auto-Accept System**: Monitors game state transitions and automates accept/ban/pick actions
- **Popup Suppression**: Smart system to hide game popups during automation
- **System Tray**: Minimize to tray with full functionality in background
- **Caching Strategy**: Multi-level caching with Caffeine (memory) and file system (persistent)

### Key Design Patterns
- **Async Loading**: CompletableFuture for non-blocking UI operations
- **Event-Driven**: JavaFX Timeline for periodic game state checks (250ms intervals)
- **Manager Pattern**: Separate managers for different concerns (SkillsManager, AvatarManager, etc.)
- **Resource Management**: Centralized resource loading with fallback mechanisms

### API Integration
- **LCU API**: Local REST API exposed by League client (uses authentication token from lockfile)
- **Data Dragon API**: Riot's CDN for champion assets and data
- **Community Dragon API**: Alternative data source for enhanced champion information
- **Tencent API**: Chinese server-specific data source

## Key Technical Details

### LCU Connection
The app connects to the League client by:
1. Finding the client process using Windows command line tools
2. Reading connection details from lockfile (riot:protocol:port:password:protocol)
3. Using Basic Auth with the provided token
4. WebSocket support for real-time events (connection monitoring)

### State Management
Game phases are tracked through LCU API:
- None → Lobby → Matchmaking → ReadyCheck → ChampSelect → InProgress → PreEndOfGame → EndOfGame

### Popup Suppression System
- Monitors game state and automatically suppresses popups during automation
- Includes error recovery: disables after consecutive failures to prevent game issues
- Configurable for ReadyCheck, Ban phase, and Pick phase

### Performance Considerations
- **Image Loading**: Async loading with placeholder support
- **Search Debouncing**: 200ms delay to prevent excessive filtering
- **Responsive Layout**: Dynamic column calculation based on window width
- **Connection Pooling**: Reuses HTTP connections for API calls
- **JVM Optimization**: G1GC with string deduplication for better memory usage

## Testing Approach

The project uses JUnit 5 with Mockito for unit tests and TestFX for UI testing. Test files should be placed in `src/test/java` following the same package structure as the main code.

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PopupSuppressionManagerTest

# Run with debug output
mvn test -X
```

## Configuration Files

- **auto-accept-config.json**: Stores user preferences for auto-accept features
- **language_preference.properties**: Language selection persistence
- **messages_*.properties**: Internationalization resources (zh_CN, en_US)
- **logback.xml**: Logging configuration (console + rolling file appender)

## Development Notes

- The application requires League of Legends client to be running for full functionality
- Uses official LCU API only - no game file modifications
- JavaFX 17 requires proper module configuration in IDE
- Champion data updates may require API version adjustments
- Main entry point is `Launcher` class which handles JavaFX initialization
- Application supports command line argument `--minimized` to start in tray

## Windows Installer

The project includes a comprehensive build system for creating Windows MSI installers:
- Self-contained: Includes Java runtime, no user prerequisites
- Automatic upgrades: Uses consistent UUID for seamless version updates
- User-level installation: No admin rights required
- Full feature integration: All resources and configurations included

## Logging

Logs are written to:
- Console: Real-time debugging output
- File: `logs/champion-selector.log` with daily rotation
- 30-day retention, 100MB total size cap