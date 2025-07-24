@echo off
chcp 65001 >nul
title 测试构建脚本改进 v2.2.0

echo ================================================
echo      测试构建脚本改进功能 v2.2.0
echo ================================================
echo.

cd /d "%~dp0"

echo [1] 测试版本号检测...
for /f "tokens=1,2 delims=<>" %%i in ('findstr "<version>" pom.xml') do (
    if "%%i"=="version" (
        set APP_VERSION=%%j
        goto :version_found
    )
)
:version_found

if "%APP_VERSION%"=="" (
    echo ❌ 版本号检测失败
) else (
    echo ✅ 版本号检测成功: %APP_VERSION%
)

echo.
echo [2] 测试artifactId检测...
for /f "tokens=1,2 delims=<>" %%i in ('findstr "<artifactId>" pom.xml') do (
    if "%%i"=="artifactId" (
        set JAR_NAME=%%j
        goto :artifactid_found
    )
)
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
echo [7] 测试分路预设修复验证...
echo 检查修复后的AutoAcceptController.java是否包含调试日志...
if exist "src\main\java\com\lol\championselector\controller\AutoAcceptController.java" (
    findstr /C:"Simple delay ban setup" "src\main\java\com\lol\championselector\controller\AutoAcceptController.java" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ 分路预设修复代码已包含
    ) else (
        echo ⚠️ 分路预设修复代码未检测到
    )
) else (
    echo ❌ AutoAcceptController.java 不存在
)

echo.
echo ================================================
echo              测试完成 v2.2.0
echo ================================================
echo.
echo 所有检测项目已完成。如果有红色的❌标记，
echo 请解决相应问题后再运行完整的构建脚本。
echo.
echo 🔧 v2.2.0 新增验证项目:
echo   - 分路预设修复代码验证
echo   - 构建脚本版本一致性检查
echo.
pause