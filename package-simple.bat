@echo off
chcp 65001 >nul
title LOL Helper v2.2.0 - 简单打包器

REM 切换到脚本所在目录
cd /d "%~dp0"

echo ================================================
echo     LOL Helper v2.2.0 简单打包器
echo ================================================
echo.
echo 新功能预览:
echo ✅ 🆕 分路预设功能 - 按分路自动配置ban/pick英雄
echo ✅ 🆕 智能分路检测 - 自动检测游戏内分路并应用预设  
echo ✅ 🆕 可视化分路配置管理 - 完整的英雄池配置界面
echo ✅ 智能弹窗抑制、自动连接重连、系统托盘等功能
echo.

REM 设置变量
set APP_VERSION=2.2.0
set JAR_NAME=lol-auto-ban-pick-tool-2.2.0-shaded.jar

echo [1/2] 检查生成的Fat JAR文件...
if not exist "target\%JAR_NAME%" (
    echo ❌ 未找到Fat JAR文件: target\%JAR_NAME%
    echo 请先运行: mvn package -DskipTests
    pause
    exit /b 1
)
echo ✅ Fat JAR文件检查通过

echo.
echo [2/2] 创建分发包...

REM 创建分发目录
if not exist "release" mkdir "release"
if not exist "release\v%APP_VERSION%" mkdir "release\v%APP_VERSION%"

REM 复制主要文件
copy "target\%JAR_NAME%" "release\v%APP_VERSION%\LOLHelper-v%APP_VERSION%.jar" >nul
if %errorlevel% neq 0 (
    echo ❌ 复制JAR文件失败
    pause
    exit /b 1
)

REM 创建启动脚本
echo @echo off > "release\v%APP_VERSION%\启动LOL Helper.bat"
echo chcp 65001 ^>nul >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo title LOL Helper v%APP_VERSION% >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo echo 正在启动 LOL Helper v%APP_VERSION%... >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo echo. >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo java --add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar "LOLHelper-v%APP_VERSION%.jar" >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo if %%errorlevel%% neq 0 ( >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo     echo ❌ 启动失败，请确保已安装 Java 17 或更高版本 >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo     echo 下载地址: https://adoptium.net/ >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo     pause >> "release\v%APP_VERSION%\启动LOL Helper.bat"
echo ) >> "release\v%APP_VERSION%\启动LOL Helper.bat"

REM 创建说明文件
echo # LOL Helper v%APP_VERSION% > "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo ## 新增功能 >> "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo ### 🆕 分路预设功能 >> "release\v%APP_VERSION%\使用说明.md"
echo - **智能分路检测**: 自动检测游戏内分路位置 >> "release\v%APP_VERSION%\使用说明.md"
echo - **预设配置**: 为每个分路预设ban/pick英雄池 >> "release\v%APP_VERSION%\使用说明.md"
echo - **可视化管理**: 完整的分路配置管理界面 >> "release\v%APP_VERSION%\使用说明.md"
echo - **优先级排序**: 支持英雄优先级和备选方案 >> "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo ## 使用方法 >> "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo 1. 确保已安装 Java 17 或更高版本 >> "release\v%APP_VERSION%\使用说明.md"
echo 2. 双击"启动LOL Helper.bat"运行程序 >> "release\v%APP_VERSION%\使用说明.md"
echo 3. 在程序中启用"分路预设"功能 >> "release\v%APP_VERSION%\使用说明.md"
echo 4. 点击"编辑配置"管理各分路的英雄池 >> "release\v%APP_VERSION%\使用说明.md"
echo 5. 进入游戏后程序会自动检测分路并应用预设 >> "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo ## 系统要求 >> "release\v%APP_VERSION%\使用说明.md"
echo. >> "release\v%APP_VERSION%\使用说明.md"
echo - Windows 10/11 >> "release\v%APP_VERSION%\使用说明.md"
echo - Java 17+ >> "release\v%APP_VERSION%\使用说明.md"
echo - 英雄联盟客户端 >> "release\v%APP_VERSION%\使用说明.md"

REM 获取文件大小
for %%A in ("release\v%APP_VERSION%\LOLHelper-v%APP_VERSION%.jar") do (
    set /a size_mb=%%~zA/1024/1024
)

echo.
echo ================================================
echo          🎉 打包完成! 🎉
echo ================================================
echo.
echo 📦 分发包信息:
echo   位置: release\v%APP_VERSION%\
echo   主程序: LOLHelper-v%APP_VERSION%.jar
echo   启动脚本: 启动LOL Helper.bat
echo   文件大小: %size_mb% MB
echo.
echo 🚀 v%APP_VERSION% 新特性:
echo ✅ 🆕 分路预设功能 - 上路/打野/中路/下路/辅助独立配置
echo ✅ 🆕 智能分路检测 - 自动识别游戏内分路并应用预设
echo ✅ 🆕 可视化配置管理 - 直观的英雄池配置界面
echo ✅ 智能弹窗抑制 - 准备检查/Ban/Pick阶段弹窗控制
echo ✅ 自动连接重连 - 断线自动重连，稳定连接
echo ✅ 系统托盘支持 - 最小化到托盘，开机自启
echo ✅ 中英文双语 - 完整的多语言支持
echo.
echo 📋 分发方法:
echo 1. 将 release\v%APP_VERSION%\ 整个文件夹分享给用户
echo 2. 用户双击"启动LOL Helper.bat"即可运行
echo 3. 首次运行会自动创建配置文件
echo.
echo 💡 注意: 用户需要安装 Java 17+ 才能运行
echo.
pause