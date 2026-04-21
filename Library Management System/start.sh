#!/bin/bash

# 图书管理系统启动脚本
# 作者: 图书管理系统开发团队
# 版本: 1.0.0

echo "📚 图书管理系统启动脚本"
echo "=========================="

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误: 未找到Python3，请先安装Python3"
    exit 1
fi

# 检查pip是否安装
if ! command -v pip3 &> /dev/null; then
    echo "❌ 错误: 未找到pip3，请先安装pip3"
    exit 1
fi

echo "✅ Python环境检查通过"

# 进入后端目录
cd backend

# 检查虚拟环境是否存在
if [ ! -d "venv" ]; then
    echo "📦 创建虚拟环境..."
    python3 -m venv venv
    if [ $? -ne 0 ]; then
        echo "❌ 虚拟环境创建失败"
        exit 1
    fi
    echo "✅ 虚拟环境创建成功"
fi

# 激活虚拟环境
echo "🔄 激活虚拟环境..."
source venv/bin/activate

# 升级pip
echo "⬆️ 升级pip..."
pip install --upgrade pip

# 安装依赖
echo "📦 安装项目依赖..."
pip install -r requirements.txt
if [ $? -ne 0 ]; then
    echo "❌ 依赖安装失败"
    exit 1
fi
echo "✅ 依赖安装完成"

# 检查数据库文件是否存在
if [ ! -f "library.db" ]; then
    echo "🗄️ 初始化数据库..."
    python schema_init.py
    if [ $? -ne 0 ]; then
        echo "❌ 数据库初始化失败"
        exit 1
    fi
    echo "✅ 数据库初始化完成"
else
    echo "ℹ️ 数据库已存在，跳过初始化"
fi

# 启动Flask应用
echo "🚀 启动图书管理系统..."
echo "📝 访问地址: http://127.0.0.1:5001"
echo "👤 管理员账号: admin / admin123"
echo "👤 普通用户: user / user123"
echo "⏹️ 按 Ctrl+C 停止服务"
echo "=========================="

python app.py
