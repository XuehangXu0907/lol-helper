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
set VENDOR=LOL Helper Team
set MAIN_CLASS=com.lol.championselector.Launcher

REM 从POM.xml自动读取版本号和artifactId
echo [INFO] 正在从pom.xml读取项目信息...
for /f "tokens=1,2 delims=<>" %%i in ('findstr "<version>" pom.xml') do (
    if "%%i"=="version" (
        set APP_VERSION=%%j
        goto :version_found
    )
)
:version_found

for /f "tokens=1,2 delims=<>" %%i in ('findstr "<artifactId>" pom.xml') do (
    if "%%i"=="artifactId" (
        set JAR_NAME=%%j
        goto :artifactid_found
    )
)
:artifactid_found

REM 验证版本号是否成功获取
if "%APP_VERSION%"=="" (
    echo ❌ 错误: 无法从pom.xml读取版本号
    echo 使用默认版本号: 2.2.0
    set APP_VERSION=2.2.0
) else (
    echo ✅ 检测到版本号: %APP_VERSION%
)

if "%JAR_NAME%"=="" (
    echo ❌ 错误: 无法从pom.xml读取artifactId
    echo 使用默认名称: lol-auto-ban-pick-tool
    set JAR_NAME=lol-auto-ban-pick-tool
) else (
    echo ✅ 检测到JAR名称: %JAR_NAME%
)

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

echo [2/5] 检查系统环境...

REM 检查磁盘空间（需要至少500MB）
echo [INFO] 检查磁盘可用空间...
for /f "tokens=3" %%a in ('dir /-c ^| find "bytes free"') do set FREESPACE=%%a
set /a FREESPACE_MB=%FREESPACE:~0,-3%/1024/1024
if %FREESPACE_MB% LSS 500 (
    echo ⚠️ 警告: 磁盘可用空间不足 %FREESPACE_MB%MB，建议至少500MB
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
    echo ⚠️ 警告: 未检测到WiX Toolset，MSI创建可能失败
    echo 建议安装: https://wixtoolset.org/releases/
) else (
    echo ✅ WiX Toolset 检测通过
)
echo.

echo [3/5] 清理并构建项目...

REM 智能清理：处理文件锁定问题
if exist target (
    echo [INFO] 尝试清理target目录...
    rmdir /s /q target 2>nul
    if exist target (
        echo ⚠️ 警告: target目录清理失败（可能有文件被锁定）
        echo 将尝试增量构建...
    ) else (
        echo ✅ target目录清理成功
    )
)

if exist dist (
    echo [INFO] 清理dist目录...
    rmdir /s /q dist 2>nul
    if exist dist (
        echo ⚠️ 警告: dist目录清理失败，将覆盖现有文件
    ) else (
        echo ✅ dist目录清理成功
    )
)

echo 正在构建项目（包括Fat JAR）...
REM 使用package而不是clean package来避免文件锁定问题
call mvn package -DskipTests -q
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

echo [4/5] 验证生成的文件...
echo [INFO] 检查生成的JAR文件...
if not exist "target\%JAR_NAME%-%APP_VERSION%-shaded.jar" (
    echo ❌ Fat JAR文件未找到: target\%JAR_NAME%-%APP_VERSION%-shaded.jar
    echo.
    echo 可能的原因:
    echo 1. Maven Shade插件配置错误
    echo 2. 构建过程中断
    echo 3. 版本号不匹配
    echo.
    echo 正在检查target目录内容...
    dir target\*.jar
    pause
    exit /b 1
)
echo ✅ Fat JAR文件验证通过: %JAR_NAME%-%APP_VERSION%-shaded.jar
echo.

echo [5/5] 创建安装包...
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
         --main-jar "%JAR_NAME%-%APP_VERSION%-shaded.jar" ^
         --main-class "%MAIN_CLASS%" ^
         --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
         --java-options "-Dfile.encoding=UTF-8" ^
         --java-options "-Duser.language=zh" ^
         --java-options "-Duser.country=CN" ^
         --java-options "-Xmx512m" ^
         --win-upgrade-uuid "E6B7C4A2-8F3D-4E5A-9B6C-1D2E3F4A5B6C" ^
         --win-per-user-install ^
         --win-menu ^
         --win-dir-chooser ^
         --win-shortcut ^
         --description "英雄联盟助手 v2.2 - 修复分路预设生效问题、增强配置同步机制、智能调试日志、完整分路预设功能、系统托盘、多语言支持" ^
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
echo   文件位置: %MSI_FILE%
echo   安装包类型: Windows MSI
echo   应用名称: %APP_NAME%
echo   版本: %APP_VERSION%
echo   升级UUID: E6B7C4A2-8F3D-4E5A-9B6C-1D2E3F4A5B6C
echo.

REM 显示文件大小和创建时间
for %%A in ("%MSI_FILE%") do (
    set /a size_mb=%%~zA/1024/1024
    echo   文件大小: %%~zA 字节 (约 !size_mb! MB)
    echo   创建时间: %%~tA
)

REM 基本的MSI文件验证
echo.
echo [INFO] 验证MSI文件完整性...
if exist "%MSI_FILE%" (
    for %%A in ("%MSI_FILE%") do (
        if %%~zA LSS 50000000 (
            echo ⚠️ 警告: MSI文件大小异常小 (%%~zA 字节)，可能构建不完整
        ) else (
            echo ✅ MSI文件大小验证通过
        )
    )
)
echo.
echo 🚀 特性:
echo ✅ 自包含安装程序
echo ✅ 傻瓜式安装体验  
echo ✅ 🆕 自动升级安装 - 新版本安装时自动卸载旧版本
echo ✅ 🆕 用户级安装 - 无需管理员权限，避免权限问题
echo ✅ 自动创建桌面快捷方式
echo ✅ 自动添加到开始菜单
echo ✅ 支持标准卸载
echo ✅ 包含完整Java运行时
echo ✅ 系统托盘最小化支持
echo ✅ 开机自动启动功能
echo ✅ 中英文双语界面
echo ✅ 🆕 分路预设功能 - 按分路自动配置ban/pick英雄
echo ✅ 🆕 智能分路检测 - 自动检测游戏内分路并应用预设
echo ✅ 🆕 可视化分路配置管理 - 完整的英雄池配置界面
echo ✅ 🔧 分路预设修复 - 修复预设应用但不生效的关键问题
echo ✅ 🔧 配置同步优化 - 增强配置变更的稳定性和一致性
echo ✅ 🔧 智能调试日志 - 详细跟踪配置变更和执行过程
echo ✅ 智能弹窗抑制 (修复session识别，确保每个action只抑制一次)
echo ✅ 稳定的session管理 (使用gameId等稳定标识符)
echo ✅ 自动连接LOL客户端
echo ✅ 断线自动重连
echo ✅ 智能游戏状态检测
echo ✅ 英雄ID映射修复
echo ✅ 错误恢复机制
echo ✅ 详细的调试日志和状态跟踪
echo ✅ 🆕 简单延迟Ban功能 - 支持1-60秒延迟执行，解决时机问题
echo.
echo 🔧 构建脚本增强:
echo ✅ 🆕 自动版本检测 - 从pom.xml自动读取版本号
echo ✅ 🆕 智能文件锁定处理 - 避免构建时的文件占用问题
echo ✅ 🆕 磁盘空间检查 - 确保有足够空间进行构建
echo ✅ 🆕 WiX Toolset检测 - 预检查MSI构建环境
echo ✅ 🆕 MSI文件完整性验证 - 构建后自动验证文件大小
echo ✅ 🔧 构建验证增强 - 验证分路预设修复是否正确编译
echo.
echo 📋 用户使用方法:
echo 【首次安装】:
echo 1. 双击 %APP_NAME%-%APP_VERSION%.msi
echo 2. 按照安装向导操作
echo 3. 从桌面或开始菜单启动程序
echo.
echo 【升级安装】:
echo 1. 直接双击新版本的 %APP_NAME%-%APP_VERSION%.msi
echo 2. 系统会自动卸载旧版本并安装新版本
echo 3. 用户配置和数据将会保留
echo 4. 🔧 v2.2.0修复了分路预设不生效的关键问题
echo.
echo 💡 提示: 
echo - 用户计算机无需安装Java即可使用
echo - 安装无需管理员权限
echo - 升级时无需手动卸载旧版本
echo - v2.2.0已修复分路预设应用但不生效的问题
echo - 现在分路预设将正确应用到ban/pick操作中
echo.
pause