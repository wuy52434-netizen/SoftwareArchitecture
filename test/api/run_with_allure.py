# -*- coding: utf-8 -*-
"""一键运行全套接口自动化 + 生成 Allure 测试报告。

步骤：
  1. pytest 收集全部 test_*.py，输出 allure-results/ (JSON) 与 junit.xml
  2. 用 allure 命令行 generate 生成 allure-report/ (HTML)
  3. 打开报告

用法: python run_with_allure.py            # 全量
      python run_with_allure.py test_auth.py test_books.py
兼容: Windows + 复用本机既有 allure 命令行(来自其它 node_modules) 或 --allure-cli 指定。
"""
import os, shutil, subprocess, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
ALLURE_RESULTS = os.path.join(ROOT, "allure-results")
ALLURE_REPORT = os.path.join(ROOT, "allure-report")
PY = r"C:/Users/ChenZhiBang/.workbuddy/binaries/python/envs/default/Scripts/python.exe"

# 探测可用的 allure 命令行。Python subprocess 在 Windows 只能执行 .cmd/.exe/.bat，
# 故优先找 .cmd（bash shim 不可直接被 subprocess 调用）。
ALLURE_CANDIDATES = [
    r"E:\test\housekeeping-miniprogram\qa\ui-automation\node_modules\.bin\allure.cmd",
    r"C:\Users\ChenZhiBang\.workbuddy\binaries\node\workspace\node_modules\.bin\allure.cmd",
    "allure",  # PATH
]

def find_allure():
    for c in ALLURE_CANDIDATES:
        if os.path.exists(c):
            return c
        if shutil.which(c):
            return c
    return None

def main():
    targets = sys.argv[1:] or ["test_auth.py", "test_books.py", "test_borrow.py",
                               "test_users.py", "test_search.py", "test_stats.py", "test_settings.py"]
    # 清空旧 allure-results
    if os.path.exists(ALLURE_RESULTS):
        shutil.rmtree(ALLURE_RESULTS)
    os.makedirs(ALLURE_RESULTS, exist_ok=True)

    # 1) pytest 跑用例并产出 allure 结果
    cmd = [PY, "-m", "pytest", *targets,
           f"--alluredir={ALLURE_RESULTS}",
           "--html=report.html", "--self-contained-html",
           "--junitxml=junit.xml", "-q"]
    print(">>> " + " ".join(cmd))
    rc = subprocess.call(cmd, cwd=ROOT)
    print(f"\npytest 退出码={rc}")

    # 2) 生成 Allure HTML 报告（用相对路径，规避 Windows 含空格绝对地址问题）
    allure = find_allure()
    if not allure:
        print("\n[!] 未找到 allure 命令行，跳过 HTML 生成（已产出 allure-results JSON，可用 IDE 插件打开）")
        return rc
    if os.path.exists(ALLURE_REPORT):
        shutil.rmtree(ALLURE_REPORT)
    # 在 ROOT 目录内以短命令执行，规避空格路径问题；.cmd wrapper 需走 shell
    shell = allure.endswith(".cmd")
    cmd = f'"{allure}" generate allure-results -o allure-report --clean' if shell else \
          [allure, "generate", "allure-results", "-o", "allure-report", "--clean"]
    subprocess.call(cmd, cwd=ROOT, shell=shell)
    idx = os.path.join(ALLURE_REPORT, "index.html")
    if os.path.exists(idx):
        print(f"\n[OK] Allure 报告已生成: {idx}")
    return rc

if __name__ == "__main__":
    sys.exit(main())