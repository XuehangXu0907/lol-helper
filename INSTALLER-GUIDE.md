# LOL Helper - Foolproof Windows Installer Guide

## 🎯 Goal
Create a double-click installable Windows installer that requires zero technical knowledge from users.

## 📋 Build Steps

### 1. Environment Setup
Ensure your development environment has:
- **Java 17 or higher** (Full JDK, not JRE)
- **Maven 3.6+**
- **WiX Toolset** (Optional, for MSI packages)

### 2. One-Click Installer Generation
```bash
# Double-click or run in command line
build-installer.bat
```

This script automatically:
- ✅ Checks Java version
- ✅ Builds the project
- ✅ Creates custom JRE
- ✅ Generates Windows installer
- ✅ Shows detailed results

### 3. Get Your Installer
The generated installer is located at:
```
dist/LOLHelper-2.1.0.msi
```

## 📦 Installer Features

### User Experience
- **Foolproof Installation**: Double-click → Next → Done
- **No Java Required**: Bundled complete Java runtime
- **Auto Shortcuts**: Desktop and Start Menu
- **Standard Uninstall**: Control Panel uninstaller

### Technical Features
- **Self-contained**: ~100MB with all dependencies
- **Native Install**: Windows MSI format
- **Multi-language**: Chinese and English support
- **Complete Resources**: All champion avatars and skill icons
- **Fixed Popup Suppression**: Session-based logic ensures each action is suppressed only once
- **Auto Connection**: Automatic League client connection and reconnection
- **System Tray Support**: Minimize to tray with notification support

## 🚀 Distribution Options

### Option 1: Direct MSI Distribution
1. Send `LOLHelper-2.1.0.msi` to users
2. Users double-click the installer
3. Follow wizard to complete installation

### Option 2: Create Download Page
Create a simple HTML page:
```html
<!DOCTYPE html>
<html>
<head>
    <title>LOL Helper Download</title>
</head>
<body>
    <h1>LOL Helper v2.1.0</h1>
    <p>League of Legends Auto Accept/Ban/Pick Tool with Fixed Popup Suppression</p>
    <a href="LOLHelper-2.1.0.msi" download>
        <button>Download Installer (~100MB)</button>
    </a>
    <h2>What's New in v2.1.0</h2>
    <ul>
        <li>✅ Fixed popup suppression logic - each action suppressed only once</li>
        <li>✅ Stable session management using gameId</li>
        <li>✅ Auto connection and reconnection features</li>
        <li>✅ System tray support with notifications</li>
        <li>✅ Enhanced debugging and logging</li>
    </ul>
    <h2>System Requirements</h2>
    <ul>
        <li>Windows 10/11 (64-bit)</li>
        <li>~150MB available disk space</li>
        <li>No Java installation required</li>
    </ul>
</body>
</html>
```

## 🔧 Customization

### Add Application Icon
1. Prepare 256x256 pixel PNG image
2. Convert to ICO format
3. Save as `src/main/resources/icon/app-icon.ico`
4. Re-run `build-installer.bat`

### Modify Application Info
Edit variables in `build-installer.bat`:
```batch
set APP_NAME=YourAppName
set APP_VERSION=VersionNumber
set VENDOR=YourCompanyName
```

## ❗ Troubleshooting

### Q: Build fails with Java version error
**A:** Ensure you have JDK 17+, not JRE. Download: https://adoptium.net/

### Q: jpackage fails
**A:** 
1. Check if WiX Toolset is installed
2. Try using `--type exe` instead of `--type msi`

### Q: Installer too large
**A:** 
- This is normal as it includes complete Java runtime
- Benefit: Users need no dependencies

### Q: Want smaller distribution
**A:** 
Use traditional approach:
1. Require users to install Java 17+
2. Distribute Fat JAR file
3. Provide run scripts

## 📊 File Size Comparison

| Method | File Size | User Requirements | Installation Complexity |
|--------|-----------|-------------------|------------------------|
| MSI Installer | ~100MB | None | Minimal |
| Fat JAR | ~20MB | Java required | Medium |
| Regular JAR | ~5MB | Java + dependencies | Complex |

## 🎉 Done!

You now have a completely self-contained Windows installer that can be easily distributed to any Windows user!

Users only need to:
1. Download the MSI file
2. Double-click to install
3. Launch from desktop

That's it!