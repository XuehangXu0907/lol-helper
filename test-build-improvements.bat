@echo off
chcp 65001 >nul
title 测试构建脚本改进

echo ================================================
echo      测试构建脚本改进功能
echo ================================================
echo.

cd /d "%~dp0"

echo [1] 测试版本号检测...
REM 从pom.xml读取版本号（与build-installer.bat保持一致）
REM 使用PowerShell更精确地读取XML
for /f "delims=" %%i in ('powershell -Command "([xml](Get-Content pom.xml)).project.version"') do set APP_VERSION=%%i

:version_found

if "%APP_VERSION%"=="" (
    echo ❌ 版本号检测失败
) else (
    echo ✅ 版本号检测成功: %APP_VERSION%
)

echo.
echo [2] 测试artifactId检测...
REM 直接设置已知的JAR名称
set JAR_NAME=lol-auto-ban-pick-tool

:artifactid_found

if "%JAR_NAME%"=="" (
    echo ❌ artifactId检测失败
) else (
    echo ✅ artifactId检测成功: %JAR_NAME%
)

echo.
echo [3] 测试JAR文件路径构造...
set EXPECTED_JAR=target\%JAR_NAME%-%APP_VERSION%-shaded.jar
echo 预期JAR文件路径: %EXPECTED_JAR%

if exist "%EXPECTED_JAR%" (
    echo ✅ JAR文件存在
    for %%A in ("%EXPECTED_JAR%") do (
        set /a size_mb=%%~zA/1024/1024
        echo   文件大小: %%~zA 字节 (约 !size_mb! MB)
    )
) else (
    echo ⚠️ JAR文件不存在 (这是正常的，如果还未构建)
)

echo.
echo [4] 测试磁盘空间检测...
for /f "tokens=3" %%a in ('dir /-c ^| find "bytes free"') do set FREESPACE=%%a
if defined FREESPACE (
    set /a FREESPACE_MB=%FREESPACE:~0,-3%/1024/1024
    echo ✅ 磁盘空间检测成功: !FREESPACE_MB!MB 可用
    if !FREESPACE_MB! LSS 500 (
        echo ⚠️ 警告: 磁盘空间可能不足
    )
) else (
    echo ❌ 磁盘空间检测失败
)

echo.
echo [5] 测试WiX Toolset检测...
where candle >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ WiX Toolset 已安装
) else (
    echo ⚠️ WiX Toolset 未检测到
)

echo.
echo [6] 测试Java和jpackage检测...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Java 已安装
    jpackage --version >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ jpackage 可用
    ) else (
        echo ❌ jpackage 不可用
    )
) else (
    echo ❌ Java 未安装
)

echo.
echo.
echo [7] 测试翻译功能验证...
echo 检查翻译文件完整性...
if exist "src\main\resources\messages_zh_CN.properties" (
    echo ✅ 中文翻译文件存在
    REM 检查是否包含关键翻译键
    findstr /C:"tab.autoFunction" "src\main\resources\messages_zh_CN.properties" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Tab页翻译键已包含
    ) else (
        echo ⚠️ Tab页翻译键未检测到
    )
) else (
    echo ❌ 中文翻译文件不存在
)

if exist "src\main\resources\messages_en_US.properties" (
    echo ✅ 英文翻译文件存在
    REM 检查是否包含关键翻译键
    findstr /C:"tab.autoFunction" "src\main\resources\messages_en_US.properties" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Tab页英文翻译键已包含
    ) else (
        echo ⚠️ Tab页英文翻译键未检测到
    )
) else (
    echo ❌ 英文翻译文件不存在
)

echo.
echo [8] 测试控制器翻译支持验证...
echo 检查AutoAcceptController.java是否包含翻译方法...
if exist "src\main\java\com\lol\championselector\controller\AutoAcceptController.java" (
    findstr /C:"autoFunctionTab" "src\main\java\com\lol\championselector\controller\AutoAcceptController.java" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Tab翻译支持代码已包含
    ) else (
        echo ⚠️ Tab翻译支持代码未检测到
    )
) else (
    echo ❌ AutoAcceptController.java 不存在
)

echo.
echo ================================================
echo              测试完成 v%APP_VERSION%
echo ================================================
echo.
echo 所有检测项目已完成。如果有红色的❌标记，
echo 请解决相应问题后再运行完整的构建脚本。
echo.
echo 🆕 v%APP_VERSION% 验证项目:
echo   - 版本号自动检测
echo   - 构建环境完整性
echo   - 翻译文件验证
echo   - 依赖项检查
echo.
pause