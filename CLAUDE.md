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

# Package the application
mvn package

# Run a single test class
mvn test -Dtest=YourTestClassName

# Run a single test method
mvn test -Dtest=YourTestClassName#yourTestMethod
```

## Architecture Overview

The application follows a modular MVC architecture:

### Core Components
- **LCU Integration**: Uses OkHttp to communicate with League Client Update API for game state monitoring
- **Champion Management**: Handles champion data, avatars, and skills through dedicated managers
- **Auto-Accept System**: Monitors game state transitions and automates accept/ban/pick actions
- **Caching Strategy**: Multi-level caching with Caffeine (memory) and file system (persistent)

### Key Design Patterns
- **Async Loading**: CompletableFuture for non-blocking UI operations
- **Event-Driven**: JavaFX Timeline for periodic game state checks
- **Manager Pattern**: Separate managers for different concerns (SkillsManager, AvatarManager, etc.)

### API Integration
- **LCU API**: Local REST API exposed by League client (uses authentication token from lockfile)
- **Data Dragon API**: Riot's CDN for champion assets and data
- **Community Dragon API**: Alternative data source for enhanced champion information
- **Tencent API**: Chinese server-specific data source

## Key Technical Details

### LCU Connection
The app connects to the League client by:
1. Finding the client process
2. Reading connection details from lockfile
3. Using Basic Auth with the provided token
4. WebSocket support for real-time events

### State Management
Game phases are tracked through LCU API:
- None → Lobby → Matchmaking → ReadyCheck → ChampSelect → InProgress → PreEndOfGame → EndOfGame

### Performance Considerations
- **Image Loading**: Async loading with placeholder support
- **Search Debouncing**: 200ms delay to prevent excessive filtering
- **Responsive Layout**: Dynamic column calculation based on window width
- **Connection Pooling**: Reuses HTTP connections for API calls

## Testing Approach

The project uses JUnit 5 with Mockito for unit tests and TestFX for UI testing. Test files should be placed in `src/test/java` following the same package structure as the main code.

## Configuration Files

- **auto-accept-config.json**: Stores user preferences for auto-accept features
- **language_preference.properties**: Language selection persistence
- **messages_*.properties**: Internationalization resources (zh_CN, en_US)
- **logback.xml**: Logging configuration

## Development Notes

- The application requires League of Legends client to be running for full functionality
- Uses official LCU API only - no game file modifications
- JavaFX 17 requires proper module configuration in IDE
- Champion data updates may require API version adjustments