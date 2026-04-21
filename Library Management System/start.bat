@echo off
chcp 65001 >nul
title 图书管理系统启动脚本

echo 📚 图书管理系统启动脚本
echo ==========================

REM 检查Python是否安装
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Python，请先安装Python
    pause
    exit /b 1
)

REM 检查pip是否安装
pip --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到pip，请先安装pip
    pause
    exit /b 1
)

echo ✅ Python环境检查通过

REM 进入后端目录
cd backend

REM 检查虚拟环境是否存在
if not exist "venv" (
    echo 📦 创建虚拟环境...
    python -m venv venv
    if %errorlevel% neq 0 (
        echo ❌ 虚拟环境创建失败
        pause
        exit /b 1
    )
    echo ✅ 虚拟环境创建成功
)

REM 激活虚拟环境
echo 🔄 激活虚拟环境...
call venv\Scripts\activate.bat

REM 升级pip
echo ⬆️ 升级pip...
python -m pip install --upgrade pip

REM 安装依赖
echo 📦 安装项目依赖...
pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo ❌ 依赖安装失败
    pause
    exit /b 1
)
echo ✅ 依赖安装完成

REM 检查数据库文件是否存在
if not exist "library.db" (
    echo 🗄️ 初始化数据库...
    python schema_init.py
    if %errorlevel% neq 0 (
        echo ❌ 数据库初始化失败
        pause
        exit /b 1
    )
    echo ✅ 数据库初始化完成
) else (
    echo ℹ️ 数据库已存在，跳过初始化
)

REM 启动Flask应用
echo 🚀 启动图书管理系统...
echo 📝 访问地址: http://127.0.0.1:5001
echo 👤 管理员账号: admin / admin123
echo 👤 普通用户: user / user123
echo ⏹️ 按 Ctrl+C 停止服务
echo ==========================

python app.py

pause
