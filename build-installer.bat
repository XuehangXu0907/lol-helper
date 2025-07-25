@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title LOL Helper v2.2.4 - MSI安装包生成器

REM 切换到脚本所在目录
cd /d "%~dp0"

echo ===============================================
echo    LOL Helper v2.2.4 - MSI安装包生成器
echo ===============================================
echo.
echo 🚀 此脚本将创建一个完全自包含的Windows安装程序
echo 📦 用户无需安装Java，双击即可安装和使用
echo ✨ 包含最新优化的构建配置和所有功能特性
echo.
echo 当前工作目录: %CD%
echo.

REM 检查是否在正确的项目目录
if not exist "pom.xml" (
    echo ❌ 错误: 未找到 pom.xml 文件
    echo 请确保在项目根目录下运行此脚本
    echo.
    pause
    exit /b 1
)
echo ✅ 项目目录验证通过
echo.

REM 设置变量
set APP_NAME=LOLHelper
set VENDOR=LOL Helper Team
set MAIN_CLASS=com.lol.championselector.Launcher
set DESCRIPTION="LOL Helper - Advanced League of Legends automation tool with champion selector, auto-accept, position presets, smart popup suppression, system tray support, and full Chinese-English interface translation"

REM 从POM.xml自动读取版本号和artifactId
echo [INFO] 正在从pom.xml读取项目信息...

REM 从pom.xml读取版本号
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { ([xml](Get-Content pom.xml)).project.version } catch { Write-Output '' }"') do set APP_VERSION=%%i
if "%APP_VERSION%"=="" (
    echo ❌ 错误: 无法从pom.xml读取版本号
    echo 请检查pom.xml文件格式是否正确
    pause
    exit /b 1
)
echo [INFO] 版本号: %APP_VERSION%

REM 从pom.xml读取artifactId
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { ([xml](Get-Content pom.xml)).project.artifactId } catch { Write-Output '' }"') do set JAR_NAME=%%i
if "%JAR_NAME%"=="" (
    echo ❌ 错误: 无法从pom.xml读取artifactId
    echo 请检查pom.xml文件格式是否正确
    pause
    exit /b 1
)
echo [INFO] JAR名称: %JAR_NAME%

echo.
echo 🎯 构建配置:
echo   - 应用名称: %APP_NAME%
echo   - 版本号: %APP_VERSION%
echo   - JAR文件: %JAR_NAME%-%APP_VERSION%-shaded.jar
echo   - 主类: %MAIN_CLASS%
echo.

echo [1/5] 检查构建环境...

REM 检查Java版本
echo [INFO] 检查Java版本...
java -version 2>&1 | findstr /C:"17" /C:"18" /C:"19" /C:"20" /C:"21" >nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 需要Java 17或更高版本
    echo.
    echo 请安装Java 17+:
    echo - 推荐: Eclipse Temurin JDK 17+ (https://adoptium.net/)
    echo - 或者: Oracle JDK 17+ (https://www.oracle.com/java/technologies/downloads/)
    echo.
    pause
    exit /b 1
)

REM 检查jpackage是否可用
jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: jpackage不可用
    echo 请确保使用完整的JDK 17+，而不是JRE
    echo jpackage是创建原生安装包的必需工具
    pause
    exit /b 1
)
echo ✅ Java环境检查通过

REM 检查磁盘空间（需要至少1GB）
echo [INFO] 检查磁盘可用空间...
for /f "skip=1 tokens=3" %%a in ('wmic logicaldisk where caption="%~d0" get size^,freespace /value ^| findstr "FreeSpace"') do (
    set /a FREESPACE_MB=%%a/1024/1024 2>nul
)
if not defined FREESPACE_MB set FREESPACE_MB=2000
if %FREESPACE_MB% LSS 1000 (
    echo ⚠️ 警告: 磁盘可用空间不足 %FREESPACE_MB%MB，建议至少1GB
    echo 继续构建可能会失败，是否继续？ [Y/N]
    set /p CONTINUE=
    if /i not "%CONTINUE%"=="Y" exit /b 1
) else (
    echo ✅ 磁盘空间检查通过: %FREESPACE_MB%MB 可用
)

REM 检查WiX Toolset（用于MSI创建）
echo [INFO] 检查WiX Toolset...
where candle >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️ 警告: 未检测到WiX Toolset
    echo 推荐安装WiX v3.11+以确保MSI创建成功
    echo 下载地址: https://wixtoolset.org/releases/
    echo.
    echo 继续创建安装包吗？ [Y/N]
    set /p CONTINUE_WIX=
    if /i not "%CONTINUE_WIX%"=="Y" exit /b 1
) else (
    echo ✅ WiX Toolset 检测通过
)
echo.

echo [2/5] 验证构建文件...

REM 验证Fat JAR文件是否存在
set SHADED_JAR=target\%JAR_NAME%-%APP_VERSION%-shaded.jar
echo [INFO] 检查Fat JAR文件: %SHADED_JAR%
if not exist "%SHADED_JAR%" (
    echo ❌ 错误: Fat JAR文件不存在
    echo 文件路径: %SHADED_JAR%
    echo.
    echo 🔧 解决方案:
    echo 1. 运行 quick-build.bat 生成JAR文件
    echo 2. 或者运行: mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

REM 检查JAR文件大小（应该在20MB以上）
for %%A in ("%SHADED_JAR%") do (
    set /a jar_size_mb=%%~zA/1024/1024
    if !jar_size_mb! LSS 20 (
        echo ⚠️ 警告: JAR文件大小异常 (!jar_size_mb! MB)，可能构建不完整
        echo 建议重新构建JAR文件
        pause
    ) else (
        echo ✅ JAR文件验证通过 (!jar_size_mb! MB)
    )
)

echo.
echo [3/5] 准备安装包资源...

REM 创建输出目录
if not exist "dist" (
    mkdir "dist"
    echo [INFO] 创建dist目录
)

REM 检查图标文件
set ICON_PARAM=
if exist "src\main\resources\icon\icon.png" (
    set ICON_PARAM=--icon "src\main\resources\icon\icon.png"
    echo ✅ 找到应用程序图标: icon.png
) else if exist "src\main\resources\icon\app-icon.ico" (
    set ICON_PARAM=--icon "src\main\resources\icon\app-icon.ico"
    echo ✅ 找到应用程序图标: app-icon.ico
) else (
    echo ⚠️ 未找到图标文件，使用默认图标
)

REM 验证翻译文件
echo [INFO] 验证国际化文件...
if exist "src\main\resources\messages_zh_CN.properties" (
    echo ✅ 中文翻译文件存在
) else (
    echo ❌ 中文翻译文件缺失
)
if exist "src\main\resources\messages_en_US.properties" (
    echo ✅ 英文翻译文件存在
) else (
    echo ❌ 英文翻译文件缺失
)

echo.
echo [4/5] 检查运行中的进程...

REM 检查是否有运行中的LOL Helper进程
echo [INFO] 检查是否有运行中的LOL Helper进程...
tasklist /FI "IMAGENAME eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorlevel% equ 0 (
    echo ⚠️ 警告: 检测到运行中的Java进程
    echo 如果其中包含LOL Helper，可能会导致MSI安装冲突
    echo 建议在安装新版本前完全退出LOL Helper应用
    echo.
    echo 继续创建安装包吗？ [Y/N]
    set /p CONTINUE_PROC=
    if /i not "%CONTINUE_PROC%"=="Y" exit /b 1
)

echo.
echo [5/5] 创建Windows MSI安装程序...
echo [INFO] 正在使用jpackage创建MSI安装包...

REM 生成唯一的升级UUID（保持一致以支持自动升级）
set UPGRADE_UUID=B8E6A7F9-3C2D-4A58-9B14-7E6F8D9C5A12

echo 📋 安装包配置:
echo   - 类型: Windows MSI
echo   - 输入目录: target
echo   - 输出目录: dist
echo   - 升级UUID: %UPGRADE_UUID%
echo   - 安装模式: 用户级安装（无需管理员权限）
echo.

REM 运行jpackage创建MSI安装包
jpackage --type msi ^
         --input target ^
         --dest dist ^
         --name "%APP_NAME%" ^
         --app-version "%APP_VERSION%" ^
         --vendor "%VENDOR%" ^
         --main-jar "%JAR_NAME%-%APP_VERSION%-shaded.jar" ^
         --main-class "%MAIN_CLASS%" ^
         --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
         --java-options "-Dfile.encoding=UTF-8" ^
         --java-options "-Dsun.jnu.encoding=UTF-8" ^
         --java-options "-Duser.language=zh" ^
         --java-options "-Duser.country=CN" ^
         --java-options "-Xmx768m" ^
         --java-options "-Xms256m" ^
         --java-options "-XX:+UseG1GC" ^
         --java-options "-XX:+UseStringDeduplication" ^
         --java-options "-Dawt.useSystemAAFontSettings=on" ^
         --java-options "-Dswing.aatext=true" ^
         --java-options "-Dglass.win.minHiDPI=1" ^
         --java-options "-Dprism.allowhidpi=true" ^
         --java-options "-Dprism.text=t2k" ^
         --java-options "-Dsun.java2d.uiScale.enabled=true" ^
         --win-upgrade-uuid "%UPGRADE_UUID%" ^
         --win-per-user-install ^
         --win-menu ^
         --win-dir-chooser ^
         --win-shortcut ^
         --description %DESCRIPTION% ^
         --copyright "Copyright (c) 2025 LOL Helper Team. All rights reserved." %ICON_PARAM%

if %errorlevel% neq 0 (
    echo ❌ 安装包创建失败 (错误代码: %errorlevel%)
    echo.
    echo 🔧 可能的解决方案:
    echo 1. 确保有足够的磁盘空间
    echo 2. 安装 WiX Toolset v3.11+: https://wixtoolset.org/releases/
    echo 3. 尝试以管理员身份运行脚本
    echo 4. 关闭防病毒软件的实时保护
    echo 5. 确保没有LOL Helper进程正在运行
    echo.
    echo 🔧 高级故障排除:
    echo 6. 清理Windows Installer缓存:
    echo    - 以管理员身份运行: msiexec /unregister
    echo    - 然后运行: msiexec /regserver
    echo 7. 检查Windows事件查看器中的详细错误信息
    echo.
    pause
    exit /b 1
)

echo.
echo ===============================================
echo           🎉 安装包生成完成! 🎉
echo ===============================================
echo.

REM 验证生成的MSI文件
set MSI_FILE=dist\%APP_NAME%-%APP_VERSION%.msi
if not exist "%MSI_FILE%" (
    echo ❌ 错误: 安装包文件未找到
    echo 预期位置: %MSI_FILE%
    echo.
    echo 正在检查dist目录内容...
    if exist dist (
        dir dist\*.msi
    ) else (
        echo dist目录不存在
    )
    pause
    exit /b 1
)

echo 📦 安装包信息:
echo   - 文件位置: %MSI_FILE%
echo   - 安装包类型: Windows MSI
echo   - 应用名称: %APP_NAME%
echo   - 版本: %APP_VERSION%
echo   - 升级UUID: %UPGRADE_UUID%

REM 显示文件大小和创建时间
for %%A in ("%MSI_FILE%") do (
    set /a size_mb=%%~zA/1024/1024
    echo   - 文件大小: %%~zA 字节 (约 !size_mb! MB)
    echo   - 创建时间: %%~tA
)

REM MSI文件完整性验证
echo.
echo [INFO] 验证MSI文件完整性...
for %%A in ("%MSI_FILE%") do (
    if %%~zA LSS 30000000 (
        echo ⚠️ 警告: MSI文件大小异常小 (%%~zA 字节)
        echo 可能构建不完整，建议重新生成
    ) else (
        echo ✅ MSI文件大小验证通过
    )
)

echo.
echo 🚀 v%APP_VERSION% 新功能特性:
echo ✨ 完全重构的JAR打包系统
echo ✨ 优化的Maven Shade插件配置
echo ✨ 增强的构建脚本和错误处理
echo ✨ 改进的MSI安装包生成流程
echo ✨ 优化的JVM参数和G1垃圾收集器
echo ✨ 完整的中英文界面翻译
echo ✨ 智能分路预设和配置管理
echo ✨ 系统托盘和开机自启动支持
echo ✨ 自动接受/禁用/选择功能
echo ✨ 响应式UI设计和紧凑模式
echo ✨ 英雄技能详情和伤害数据
echo ✨ 多数据源支持和智能缓存
echo ✨ 智能弹窗抑制和会话管理
echo.
echo 📋 安装和升级说明:
echo.
echo 【首次安装】:
echo 1. 双击 %APP_NAME%-%APP_VERSION%.msi
echo 2. 按照安装向导完成安装
echo 3. 从桌面快捷方式或开始菜单启动
echo.
echo 【自动升级】:
echo 1. 完全退出当前版本的LOL Helper
echo    - 右键系统托盘图标选择"退出"
echo    - 或使用任务管理器结束Java进程
echo 2. 双击新版本MSI文件
echo 3. Windows Installer自动卸载旧版本并安装新版本
echo 4. 用户配置和数据将自动保留
echo.
echo 🎯 优势特性:
echo ✅ 傻瓜式安装 - 双击即可安装，无需技术知识
echo ✅ 自包含运行时 - 无需预装Java环境
echo ✅ 智能升级机制 - 自动卸载旧版本，配置数据保留
echo ✅ 用户级安装 - 无需管理员权限，避免权限问题
echo ✅ 完整功能集成 - 包含所有功能和资源文件
echo ✅ 标准卸载支持 - 通过控制面板完全卸载
echo ✅ 多语言界面 - 完整的中英文支持
echo ✅ 高性能优化 - G1垃圾收集器和JVM优化
echo.
echo 💡 技术规格:
echo   - Java Runtime: 自包含JDK 17+
echo   - JavaFX版本: 17.0.2
echo   - HTTP客户端: OkHttp 4.12.0
echo   - JSON处理: Jackson 2.16.1
echo   - 缓存引擎: Caffeine 3.1.8
echo   - 日志框架: Logback 1.4.14
echo   - 界面框架: JavaFX with FXML
echo   - 构建工具: Maven 3.6+
echo   - 打包工具: jpackage (JDK 17+)
echo.
echo 🔧 开发者信息:
echo   - 项目版本: %APP_VERSION%
echo   - 构建时间: %DATE% %TIME%
echo   - 构建环境: Windows %OS%
echo   - Maven配置: 优化的Shade插件
echo   - 目标平台: Windows 10/11 x64
echo.
pause