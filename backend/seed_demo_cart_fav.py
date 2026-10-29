# -*- coding: utf-8 -*-
"""
演示数据补充：购物车 3 件 + 收藏 2 件

为什么单独一个脚本：
    前面的 seed_demo_orders.py 造订单、seed_demo_reviews.py 造评价，各自只干一件事。
    购物车和收藏不涉及订单状态机，几行就能搞定，单独放一个，方便反复跑。

为什么需要：
    答辩时「购物车结算」是最值得现场演示的一条链路（加购 -> 结算 -> 生成订单）。
    购物车空着就得先手动加几件，多一步操作；预置几件能直接点结算。
    收藏页同理，空页面很难看。

幂等性：
    购物车按 (user, product) 去重（后端 add 接口本身就会累加数量），
    收藏用 toggle 接口，先查当前状态再决定是否切换 —— 重复跑不会翻倍。
    注意：购物车重复跑会把数量累加，所以脚本先检查购物车是否已有该商品。

运行：
    python seed_demo_cart_fav.py
前置：后端 8080 已启动
清理：
    DELETE FROM cart WHERE user_id = (SELECT id FROM sys_user WHERE username='consumer');
    DELETE FROM user_favorite WHERE user_id = (SELECT id FROM sys_user WHERE username='consumer');
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE", "http://127.0.0.1:8080/api")
SQL_FILE = os.environ.get("INIT_SQL", r"E:\agri-traceability\backend\sql\init.sql")

# 购物车预置：商品ID -> 数量（挑价格梯度不同的三件，结算页金额好看）
CART_PLAN = [(1, 1), (9, 1), (14, 2)]
# 收藏预置
FAV_PLAN = [1, 15]

passed = 0
failed = 0


def call(method, path, body=None, token=None):
    """发 JSON 请求，返回 (状态码, 响应体)；回环请求绕过系统代理"""
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=25) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}
    except Exception as e:
        return 0, {"raw": str(e)}


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


def read_seed_pwd(username):
    """从 init.sql 现场解析种子账号密码"""
    with open(SQL_FILE, encoding="utf-8") as f:
        text = f.read()
    m = re.search(r"\('%s',\s*'([^']+)'" % re.escape(username), text)
    return m.group(1) if m else None


print("\n===== 演示数据补充（购物车 / 收藏） =====\n")

pwd = read_seed_pwd("consumer")
if not pwd:
    print("解析不到 consumer 密码，检查 SQL_FILE =", SQL_FILE)
    sys.exit(1)

_, res = call("POST", "/user/login", {"username": "consumer", "password": pwd})
tok = (res.get("data") or {}).get("token") if res.get("code") == 200 else None
check("consumer 登录", bool(tok), "返回: %s" % res.get("message"))
if not tok:
    sys.exit(1)

# ---------- 1. 购物车：先看已有，避免重复累加数量 ----------
_, res = call("GET", "/cart/list", None, tok)
cart_rows = res.get("data") or []
in_cart = {((r.get("cart") or {}).get("productId")) for r in cart_rows}
check("购物车列表可读", res.get("code") == 200, "当前 %d 件" % len(cart_rows))

for pid, qty in CART_PLAN:
    if pid in in_cart:
        print("  [SKIP] 商品%d 已在购物车，跳过（避免数量翻倍）" % pid)
        continue
    _, res = call("POST", "/cart", {"productId": pid, "quantity": qty}, tok)
    check("加入购物车 商品%d×%d" % (pid, qty), res.get("code") == 200,
          "返回: %s" % res.get("message"))

_, res = call("GET", "/cart/list", None, tok)
cart_now = res.get("data") or []
check("购物车非空", len(cart_now) >= 3, "当前 %d 件" % len(cart_now))

# ---------- 2. 收藏：toggle 接口，先查状态再决定要不要切 ----------
_, res = call("GET", "/favorite/list", None, tok)
fav_rows = res.get("data") or []
fav_ids = {r.get("productId") for r in fav_rows}
print("  [INFO] 当前收藏 %d 件" % len(fav_rows))

for pid in FAV_PLAN:
    if pid in fav_ids:
        print("  [SKIP] 商品%d 已收藏，跳过" % pid)
        continue
    _, res = call("POST", "/favorite/toggle/%d" % pid, None, tok)
    check("收藏 商品%d" % pid, res.get("code") == 200, "返回: %s" % res.get("message"))

_, res = call("GET", "/favorite/list", None, tok)
check("收藏非空", len(res.get("data") or []) >= 2, "当前 %d 件" % len(res.get("data") or []))

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
if failed:
    sys.exit(1)
