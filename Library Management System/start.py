#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
图书管理系统启动脚本
作者: 图书管理系统开发团队
版本: 1.0.0
"""

import os
import sys
import subprocess
import platform

def run_command(command, cwd=None):
    """执行命令并返回结果"""
    try:
        result = subprocess.run(command, shell=True, cwd=cwd, check=True, 
                              capture_output=True, text=True)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def check_python():
    """检查Python环境"""
    print("🔍 检查Python环境...")
    
    # 检查Python版本
    version = sys.version_info
    if version.major < 3 or (version.major == 3 and version.minor < 7):
        print("❌ 错误: 需要Python 3.7或更高版本")
        return False
    
    print(f"✅ Python {version.major}.{version.minor}.{version.micro} 检查通过")
    return True

def check_pip():
    """检查pip是否可用"""
    print("🔍 检查pip...")
    
    success, _ = run_command(f"{sys.executable} -m pip --version")
    if not success:
        print("❌ 错误: pip不可用")
        return False
    
    print("✅ pip检查通过")
    return True

def create_virtual_env():
    """创建虚拟环境"""
    venv_path = os.path.join("backend", "venv")
    
    if os.path.exists(venv_path):
        print("ℹ️ 虚拟环境已存在")
        return True
    
    print("📦 创建虚拟环境...")
    success, output = run_command(f"{sys.executable} -m venv venv", cwd="backend")
    
    if not success:
        print(f"❌ 虚拟环境创建失败: {output}")
        return False
    
    print("✅ 虚拟环境创建成功")
    return True

def get_venv_python():
    """获取虚拟环境中的Python路径"""
    system = platform.system().lower()
    
    if system == "windows":
        venv_python = os.path.abspath(os.path.join("backend", "venv", "Scripts", "python.exe"))
    else:
        venv_python = os.path.abspath(os.path.join("backend", "venv", "bin", "python"))
    
    return venv_python

def get_venv_pip():
    """获取虚拟环境中的pip路径"""
    system = platform.system().lower()
    
    if system == "windows":
        venv_pip = os.path.join("backend", "venv", "Scripts", "pip.exe")
    else:
        venv_pip = os.path.join("backend", "venv", "bin", "pip")
    
    return venv_pip

def install_dependencies():
    """安装项目依赖"""
    print("📦 安装项目依赖...")
    
    venv_python = get_venv_python()
    
    # 升级pip
    print("⬆️ 升级pip...")
    success, output = run_command(f'"{venv_python}" -m pip install --upgrade pip')
    if not success:
        print(f"⚠️ pip升级失败，继续安装依赖: {output}")
    
    # 安装依赖
    success, output = run_command(f'"{venv_python}" -m pip install -r requirements.txt', cwd="backend")
    
    if not success:
        print(f"❌ 依赖安装失败: {output}")
        return False
    
    print("✅ 依赖安装完成")
    return True

def init_database():
    """初始化数据库"""
    db_path = os.path.join("backend", "library.db")
    
    if os.path.exists(db_path):
        print("ℹ️ 数据库已存在，跳过初始化")
        return True
    
    print("🗄️ 初始化数据库...")
    venv_python = get_venv_python()
    
    success, output = run_command(f'"{venv_python}" schema_init.py', cwd="backend")
    
    if not success:
        print(f"❌ 数据库初始化失败: {output}")
        return False
    
    print("✅ 数据库初始化完成")
    return True

def start_application():
    """启动应用"""
    print("🚀 启动图书管理系统...")
    print("📝 访问地址: http://127.0.0.1:5001")
    print("👤 管理员账号: admin / admin123")
    print("👤 普通用户: user / user123")
    print("⏹️ 按 Ctrl+C 停止服务")
    print("==========================")
    
    venv_python = get_venv_python()
    
    try:
        # 直接运行Flask应用，不捕获输出以便用户看到实时日志
        subprocess.run([venv_python, 'app.py'], cwd="backend", check=True)
    except KeyboardInterrupt:
        print("\n👋 服务已停止")
    except subprocess.CalledProcessError as e:
        print(f"❌ 应用启动失败: {e}")
        return False
    
    return True

def main():
    """主函数"""
    print("📚 图书管理系统启动脚本")
    print("==========================")
    
    # 检查环境
    if not check_python():
        sys.exit(1)
    
    if not check_pip():
        sys.exit(1)
    
    # 创建虚拟环境
    if not create_virtual_env():
        sys.exit(1)
    
    # 安装依赖
    if not install_dependencies():
        sys.exit(1)
    
    # 初始化数据库
    if not init_database():
        sys.exit(1)
    
    # 启动应用
    if not start_application():
        sys.exit(1)

if __name__ == "__main__":
    main()
