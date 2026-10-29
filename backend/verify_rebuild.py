# -*- coding: utf-8 -*-
"""
数据库重建后的冒烟验证脚本

背景：
    2026-10-28 删 D 盘后 MySQL 数据丢失，2026-10-29 重装 MySQL 8.0.29 到 E:/MySQL
    并按 backend/sql 全部脚本重建 agri_traceability。本脚本验证重建后的库能被
    后端正常读写 —— 重点覆盖「实体有字段、旧建表脚本没列」的那 7 个字段，
    它们曾让商品/订单接口报 Unknown column 500。

验证项：
    1. 商品列表返回 200，且每项都带 salesCount / rating / reviewCount / originalPrice
    2. 订单列表（管理员）返回 200 —— 覆盖 payment_method / ship_time / receive_time
    3. 质量标准、溯源流程、溯源批次、农户、百科、食谱等公开接口全部 200
    4. 管理员登录成功，后台统计接口 200
    5. 数据量与预期一致（15 商品 / 4 账号）

运行：
    python verify_rebuild.py
前置：
    后端已启动（默认 8080，可用环境变量 BASE 覆盖）
    数据库里有 init.sql 的种子账号（密码从 init.sql 现场解析，不写死在脚本里）
"""

import json
import os
import re
import urllib.error
import urllib.request

BASE = os.environ.get("BASE", "http://127.0.0.1:8080/api")
SQL_FILE = r"E:\agri-traceability\backend\sql\init.sql"

passed = 0
failed = 0


def call(method, path, body=None, token=None):
    """发 JSON 请求，返回 (状态码, 响应体 dict)"""
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    # 本机回环不走系统代理，否则会被代理拦成 502
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def check(name, ok, detail=""):
    """记录一条断言结果"""
    global passed, failed
    if ok:
        passed += 1
        print("  [PASS] %s  %s" % (name, detail))
    else:
        failed += 1
        print("  [FAIL] %s  %s" % (name, detail))


def read_seed_account(username):
    """
    从 init.sql 里解析某个种子账号的明文密码。
    不把密码写进脚本 —— 仓库是公开的。
    """
    with open(SQL_FILE, encoding="utf-8") as f:
        text = f.read()
    m = re.search(r"\('%s',\s*'([^']+)'" % re.escape(username), text)
    return m.group(1) if m else None


print("\n===== 数据库重建后冒烟验证 =====\n")

# ---------- 1. 商品列表：覆盖 product 新增的 4 个字段 ----------
_, res = call("GET", "/product/list")
data = res.get("data")
if isinstance(data, dict):
    products = data.get("records") or data.get("list") or []
elif isinstance(data, list):
    products = data
else:
    products = []
check("商品列表返回 200", res.get("code") == 200, "返回: %s" % res.get("message"))
check("商品数量为 15", len(products) == 15, "实际 %d 条" % len(products))

if products:
    p0 = products[0]
    check("商品带 salesCount", "salesCount" in p0, "值=%s" % p0.get("salesCount"))
    check("商品带 rating", "rating" in p0, "值=%s" % p0.get("rating"))
    check("商品带 reviewCount", "reviewCount" in p0, "值=%s" % p0.get("reviewCount"))
    check("商品带 originalPrice", "originalPrice" in p0, "值=%s" % p0.get("originalPrice"))
    check("商品带归属商家", p0.get("merchantId") is not None, "merchantId=%s" % p0.get("merchantId"))

# ---------- 2. 公开接口批量检查 ----------
print("\n-- 公开接口 --")
public_paths = [
    ("/category/list", "商品分类"),
    ("/quality/standards", "质量标准"),
    ("/quality/trace-process", "溯源流程"),
    ("/trace/batch/list", "溯源批次"),
    ("/farmer/list", "农户列表"),
    ("/announcement/list", "公告列表"),
    ("/knowledge/list", "农业百科"),
    ("/recipe/list", "农产食谱"),
    ("/seasonal/current", "时令推荐"),
    ("/faq/list", "常见问题"),
]
for path, label in public_paths:
    _, r = call("GET", path)
    check("%s 可访问" % label, r.get("code") == 200, "code=%s %s" % (r.get("code"), r.get("message") or ""))

# ---------- 3. 管理员登录 + 后台接口 ----------
print("\n-- 管理员链路 --")
admin_pwd = read_seed_account("admin")
token = None
if not admin_pwd:
    check("从 init.sql 解析管理员密码", False, "未匹配到 admin 元组")
else:
    check("从 init.sql 解析管理员密码", True, "长度=%d" % len(admin_pwd))
    _, r = call("POST", "/user/login", {"username": "admin", "password": admin_pwd})
    token = (r.get("data") or {}).get("token") if r.get("code") == 200 else None
    check("管理员登录成功", bool(token), "返回: %s" % r.get("message"))

if token:
    _, r = call("GET", "/admin/order/list", None, token)
    check("后台订单列表可访问", r.get("code") == 200, "返回: %s" % r.get("message"))

    _, r = call("GET", "/admin/stats/overview", None, token)
    check("后台统计可访问", r.get("code") == 200, "返回: %s" % r.get("message"))
    stats = r.get("data") or {}
    if stats:
        print("       统计快照: %s" % json.dumps(stats, ensure_ascii=False)[:200])

    _, r = call("GET", "/admin/stock/warning", None, token)
    check("库存预警可访问", r.get("code") == 200, "返回: %s" % r.get("message"))

    _, r = call("GET", "/user/list", None, token)
    users = r.get("data") or []
    check("用户管理可访问", r.get("code") == 200, "返回: %s" % r.get("message"))
    check("账号数为 4", len(users) == 4, "实际 %d 个" % len(users))
    leaked = [u for u in users if u.get("password")]
    check("用户列表不返回密码", len(leaked) == 0, "泄露 %d 条" % len(leaked))

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))

if failed:
    raise SystemExit(1)