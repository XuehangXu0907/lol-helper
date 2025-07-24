@echo off
chcp 65001 >nul
title LOL Helper v2.2 - Quick Build

echo ================================
echo   LOL Helper v2.2 Quick Build
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

REM 动态获取版本号
for /f "tokens=1,2 delims=<>" %%i in ('findstr "<version>" pom.xml') do (
    if "%%i"=="version" (
        set VERSION=%%j
        goto :version_found
    )
)
:version_found

echo.
if exist "target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar" (
    echo 📦 Generated files:
    echo   - target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar (Fat JAR)
    echo   - target\lol-auto-ban-pick-tool-%VERSION%.jar (Regular JAR)
    echo.
    echo 🚀 Quick test:
    echo   java -jar target\lol-auto-ban-pick-tool-%VERSION%-shaded.jar
    echo.
    echo 📋 Next steps:
    echo   1. Run build-installer.bat to create MSI installer
    echo   2. Or use Fat JAR directly for distribution
    echo.
) else (
    echo ❌ Expected JAR file not found
)

echo ================================
echo           Build Complete
echo ================================
pause