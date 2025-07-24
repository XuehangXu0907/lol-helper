@echo off
chcp 65001 >nul
title LOL Helper - Quick Build

echo ================================
echo   LOL Helper Quick Build
echo ================================
echo.

REM Switch to script directory
cd /d "%~dp0"

REM Check for pom.xml
if not exist "pom.xml" (
    echo ❌ Error: pom.xml not found
    pause
    exit /b 1
)

echo [1/2] Compiling project...
call mvn compile -q
if %errorlevel% neq 0 (
    echo ❌ Compilation failed
    pause
    exit /b 1
)
echo ✅ Compilation successful

echo.
echo [2/2] Creating executable JAR...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo ❌ Packaging failed
    pause
    exit /b 1
)
echo ✅ Packaging successful

REM 从pom.xml读取版本号（与build-installer.bat保持一致）
REM 使用PowerShell更精确地读取XML
for /f "delims=" %%i in ('powershell -Command "([xml](Get-Content pom.xml)).project.version"') do set VERSION=%%i
if "%VERSION%"=="" (
    echo ❌ 错误: 无法从pom.xml读取版本号
    echo 请检查pom.xml文件格式是否正确
    pause
    exit /b 1
)
echo [INFO] 从pom.xml读取版本号: %VERSION%

echo.
if exist "target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar" (
    echo 📦 Generated files:
    echo   - target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar (Fat JAR)
    echo   - target\lol-auto-ban-pick-tool-%VERSION%.jar (Regular JAR)
    echo.
    
    REM 验证翻译文件
    echo 🌐 Translation files verification:
    if exist "src\main\resources\messages_zh_CN.properties" (
        echo   ✅ Chinese translation file included
    ) else (
        echo   ❌ Chinese translation file missing
    )
    if exist "src\main\resources\messages_en_US.properties" (
        echo   ✅ English translation file included
    ) else (
        echo   ❌ English translation file missing
    )
    echo.
    
    echo 🚀 Quick test:
    echo   java -jar target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar
    echo.
    echo 📋 Next steps:
    echo   1. Run build-installer.bat to create MSI installer
    echo   2. Or use Fat JAR directly for distribution
    echo.
    echo 🆕 v%VERSION% 功能特性:
    echo   ✅ 自动接受/禁用/选择功能
    echo   ✅ 分路预设和智能配置
    echo   ✅ 完整中英文界面支持
    echo.
) else (
    echo ❌ Expected JAR file not found
)

echo ================================
echo           Build Complete
echo ================================
pause