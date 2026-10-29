# -*- coding: utf-8 -*-
"""
图片完整性校验：数据库里的图片地址，是否真的能落到本地文件并访问到

背景（2026-10-29 事故）：
    项目早期的图片字段全是 Trae IDE 内部文生图接口的 URL，该接口失效后对任何
    prompt 都返回同一张占位图，导致全站图片「看起来都还在、其实全是同一张」，
    比彻底 404 更难发现。恢复方案是把字段指向 frontend/public/images/ 本地文件
    （backend/sql/local_images.sql）。

    这个脚本负责守住这条线：以后新增商品 / 文章 / 食谱 / 农户，只要图片地址
    写错成外链或写成不存在的文件名，跑一次就能发现。

校验四件事：
    1. 四张表的图片字段里没有 http(s) 外链（外链随时会变，答辩环境不可控）
    2. 每一行都填了图片（空值会导致前端回退到兜底图，看起来像「图配错了」）
    3. 数据库里的 /images/xxx.jpg 在 frontend/public 下真实存在
    4. 通过前端 dev server 访问该路径返回 200（可选，起不来就只做本地文件校验）

运行：
    python verify_images.py
前置：
    无需后端；只读 MySQL + 本地文件。前端 dev server 在跑时会额外做第 4 项。
"""

import os
import subprocess
import sys
import urllib.error
import urllib.request

# ---------- 配置 ----------
DB = os.environ.get("DB_NAME", "agri_traceability")
DB_USER = os.environ.get("DB_USER", "root")
DB_PWD = os.environ.get("DB_PWD", "root")
FRONTEND_BASE = os.environ.get("FRONTEND_BASE", "http://localhost:5173")
# 前端静态资源根目录：/images/xxx.jpg 对应的物理位置
PUBLIC_DIR = os.environ.get(
    "PUBLIC_DIR", r"E:\agri-traceability\frontend\public")

# 要检查的 (表名, 图片列, 用于日志的展示列)
TARGETS = [
    ("product", "image", "name"),
    ("knowledge_article", "cover_image", "title"),
    ("recipe", "cover_image", "title"),
    ("farmer_info", "region_image", "name"),
]

passed = 0
failed = 0


def find_mysql():
    """
    定位 mysql.exe。它常常不在 PATH 里（本机装在 D:\\MySQL 时就这样），
    所以先查 PATH，再探常见安装目录。
    """
    from shutil import which
    p = which("mysql")
    if p:
        return p
    candidates = [
        r"E:\MySQL\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        r"D:\MySQL\MySQL Server 8.0\bin\mysql.exe",
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return None


def query(sql):
    """
    执行一条查询，返回二维列表（第一行是表头）。
    用 -N -B 让输出不带边框、Tab 分隔，便于解析。
    """
    exe = find_mysql()
    if not exe:
        raise RuntimeError("找不到 mysql.exe，请把 MySQL 的 bin 目录加入 PATH")
    cmd = [exe, "-u" + DB_USER, "-p" + DB_PWD,
           "--default-character-set=utf8mb4", "-N", "-B", "-e", sql, DB]
    r = subprocess.run(cmd, capture_output=True, timeout=30)
    if r.returncode != 0:
        raise RuntimeError("查询失败: " + r.stderr.decode("utf-8", errors="replace")[:300])
    out = r.stdout.decode("utf-8", errors="replace").strip()
    if not out:
        return []
    return [line.split("\t") for line in out.splitlines()]


def check(name, ok, detail=""):
    """记录断言结果"""
    global passed, failed
    if ok:
        passed += 1
        print("  [PASS] %s  %s" % (name, detail))
    else:
        failed += 1
        print("  [FAIL] %s  %s" % (name, detail))
    return ok


def http_status(path):
    """
    通过前端 dev server 请求某个资源路径，返回状态码；失败返回 0。
    回环请求要绕开系统代理，否则会被 HTTP_PROXY 拦成 502。
    """
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        req = urllib.request.Request(FRONTEND_BASE + path, method="HEAD")
        with opener.open(req, timeout=5) as resp:
            return resp.status
    except urllib.error.HTTPError as e:
        return e.code
    except Exception:
        return 0


print("\n===== 图片完整性校验 =====\n")

# 前端是否在线，决定要不要做第 4 项
frontend_up = http_status("/") == 200
print("前端 dev server: %s" % ("在线，将额外校验 HTTP 可达性" if frontend_up else "离线，只做本地文件校验"))

all_paths = set()

for table, col, label_col in TARGETS:
    print("\n-- %s.%s --" % (table, col))
    rows = query("SELECT id, %s, IFNULL(%s,'') FROM %s ORDER BY id;"
                 % (label_col, col, table))
    if not rows:
        check("%s 有数据" % table, False, "查询结果为空")
        continue
    check("%s 有数据" % table, True, "%d 行" % len(rows))

    empty, external, missing = [], [], []
    for rid, label, path in rows:
        if not path:
            empty.append("%s#%s" % (label, rid))
            continue
        if path.startswith("http://") or path.startswith("https://"):
            external.append("%s#%s -> %s" % (label, rid, path[:60]))
            continue
        # 绝对路径 /images/xxx.jpg -> 物理文件 frontend/public/images/xxx.jpg
        rel = path.lstrip("/").replace("/", os.sep)
        full = os.path.join(PUBLIC_DIR, rel)
        if not os.path.exists(full):
            missing.append("%s#%s -> %s" % (label, rid, path))
            continue
        all_paths.add(path)

    check("%s 无空图片字段" % table, not empty, "空值: " + ", ".join(empty) if empty else "全部已填")
    check("%s 无外链图片" % table, not external, "; ".join(external) if external else "全部本地路径")
    check("%s 本地文件都存在" % table, not missing, "; ".join(missing) if missing else "全部命中")

# ---------- 第 4 项：HTTP 可达性 ----------
print("\n-- HTTP 可达性 --")
if frontend_up:
    bad = []
    for p in sorted(all_paths):
        st = http_status(p)
        if st != 200:
            bad.append("%s -> %s" % (p, st))
    check("所有图片可被前端访问", not bad, "; ".join(bad) if bad else "共 %d 张全部 200" % len(all_paths))
else:
    print("  [SKIP] 前端未启动，跳过该项（起前端后重跑即可）")

# ---------- 汇总 ----------
print("\n-- 统计 --")
print("  数据库引用的本地图片共 %d 张" % len(all_paths))
total_files = 0
img_dir = os.path.join(PUBLIC_DIR, "images")
if os.path.isdir(img_dir):
    total_files = len([f for f in os.listdir(img_dir) if f.lower().endswith((".jpg", ".png", ".webp"))])
print("  frontend/public/images 下共有 %d 张图（未被引用的属备用素材）" % total_files)

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
if failed:
    sys.exit(1)
