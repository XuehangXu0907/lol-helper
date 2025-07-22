@echo off
chcp 65001 >nul
title LOL Helper - 傻瓜式安装包生成器

REM 切换到脚本所在目录
cd /d "%~dp0"

echo ================================================
echo       LOL Helper 傻瓜式安装包生成器
echo ================================================
echo.
echo 此脚本将创建一个完全自包含的Windows安装程序
echo 用户无需安装Java，双击即可安装和使用
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
set APP_VERSION=2.1.0
set VENDOR=LOL Helper Team
set MAIN_CLASS=com.lol.championselector.Launcher
set JAR_NAME=lol-auto-ban-pick-tool-2.1.0

echo [1/5] 检查Java版本...
java -version 2>&1 | findstr /C:"17" /C:"18" /C:"19" /C:"20" /C:"21" >nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 需要Java 17或更高版本
    echo.
    echo 请安装Java 17+:
    echo - 下载地址: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

REM 检查jpackage是否可用
jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: jpackage不可用
    echo 请确保使用完整的JDK 17+，而不是JRE
    pause
    exit /b 1
)
echo ✅ Java环境检查通过
echo.

echo [2/5] 清理并构建项目...
if exist target rmdir /s /q target 2>nul
if exist dist rmdir /s /q dist 2>nul

echo 正在构建项目（包括Fat JAR）...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ❌ 项目构建失败
    echo.
    echo 请检查:
    echo 1. 网络连接是否正常
    echo 2. Maven配置是否正确
    echo 3. 项目依赖是否完整
    pause
    exit /b 1
)
echo ✅ 项目构建成功
echo.

echo [3/5] 验证生成的文件...
if not exist "target\%JAR_NAME%-shaded.jar" (
    echo ❌ Fat JAR文件未找到: target\%JAR_NAME%-shaded.jar
    echo 请检查Maven Shade插件配置
    pause
    exit /b 1
)
echo ✅ Fat JAR文件验证通过
echo.

echo [4/5] 准备打包环境...
echo 使用系统Java运行时创建自包含安装包...

REM 检查新增的图标文件
if exist "src\main\resources\icon\tray-icon.png" (
    echo ✅ 找到系统托盘图标文件
) else (
    echo ⚠️ 未找到系统托盘图标文件
)

if exist "src\main\resources\icon\icon.png" (
    echo ✅ 找到应用程序图标文件
) else (
    echo ⚠️ 未找到应用程序图标文件
)

echo ✅ 打包环境准备完成
echo.

echo [5/5] 创建Windows安装程序...
echo 正在生成MSI安装包...

REM 创建输出目录
if not exist "dist" mkdir "dist"

REM 检查图标文件
set ICON_PARAM=
if exist "src\main\resources\icon\app-icon.ico" (
    set ICON_PARAM=--icon "src\main\resources\icon\app-icon.ico"
    echo ✅ 使用自定义图标
) else (
    echo ⚠️ 未找到图标文件，使用默认图标
)

REM 运行jpackage（简化版，避免模块化问题）
jpackage --type msi ^
         --input target ^
         --dest dist ^
         --name "%APP_NAME%" ^
         --app-version "%APP_VERSION%" ^
         --vendor "%VENDOR%" ^
         --main-jar "%JAR_NAME%-shaded.jar" ^
         --main-class "%MAIN_CLASS%" ^
         --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
         --java-options "-Dfile.encoding=UTF-8" ^
         --java-options "-Duser.language=zh" ^
         --java-options "-Duser.country=CN" ^
         --java-options "-Xmx512m" ^
         --win-menu ^
         --win-dir-chooser ^
         --win-shortcut ^
         --description "英雄联盟助手 v2.1 - 修复弹窗抑制逻辑、智能session管理、自动连接重连、系统托盘、多语言支持" ^
         --copyright "Copyright (c) 2025 LOL Helper Team" %ICON_PARAM%

if %errorlevel% neq 0 (
    echo ❌ 安装包创建失败
    echo.
    echo 可能的解决方案:
    echo 1. 确保有管理员权限
    echo 2. 安装 WiX Toolset: https://wixtoolset.org/releases/
    echo 3. 尝试关闭防病毒软件
    echo 4. 检查磁盘空间是否足够
    echo.
    echo 如果仍然失败，可以尝试创建EXE安装包:
    echo 将上面命令中的 --type msi 改为 --type exe
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo            🎉 安装包生成完成! 🎉
echo ================================================
echo.
echo 📦 安装包信息:
echo   文件位置: dist\%APP_NAME%-%APP_VERSION%.msi
echo   安装包类型: Windows MSI
echo   应用名称: %APP_NAME%
echo   版本: %APP_VERSION%
echo.

REM 显示文件大小
for %%A in ("dist\%APP_NAME%-%APP_VERSION%.msi") do (
    set /a size_mb=%%~zA/1024/1024
    echo   文件大小: %%~zA 字节 (约 !size_mb! MB)
)
echo.
echo 🚀 特性:
echo ✅ 自包含安装程序
echo ✅ 傻瓜式安装体验  
echo ✅ 自动创建桌面快捷方式
echo ✅ 自动添加到开始菜单
echo ✅ 支持标准卸载
echo ✅ 包含完整Java运行时
echo ✅ 系统托盘最小化支持
echo ✅ 开机自动启动功能
echo ✅ 中英文双语界面
echo ✅ 智能弹窗抑制 (修复session识别，确保每个action只抑制一次)
echo ✅ 稳定的session管理 (使用gameId等稳定标识符)
echo ✅ 自动连接LOL客户端 (新功能!)
echo ✅ 断线自动重连 (新功能!)
echo ✅ 智能游戏状态检测
echo ✅ 英雄ID映射修复
echo ✅ 错误恢复机制
echo ✅ 详细的调试日志和状态跟踪
echo.
echo 📋 用户使用方法:
echo 1. 双击 %APP_NAME%-%APP_VERSION%.msi
echo 2. 按照安装向导操作
echo 3. 从桌面或开始菜单启动程序
echo.
echo 💡 提示: 用户计算机无需安装Java即可使用
echo.
pause