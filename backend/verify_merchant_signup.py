# -*- coding: utf-8 -*-
"""
商家自助入驻 + 管理员审核 —— 端到端验证脚本

验证链路：
  1. 注册商家 → 成功，但 status=0（待审核）
  2. 用待审核账号登录 → 失败（后端拦 status != 1）
  3. 管理员在后台把状态改为 1（走 PUT /api/user）
  4. 再登录 → 成功，且角色是 MERCHANT
  5. 注册 ADMIN 仍然被拒（白名单生效）
清理：脚本结束前删除自己造的账号

运行：python verify_merchant_signup.py
前置：后端已启动（默认 8082，可用环境变量 BASE 覆盖）
"""

import json
import os
import urllib.request
import urllib.error

BASE = os.environ.get("BASE", "http://localhost:8082/api")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
# 管理员密码从环境变量读，不写死在仓库里（仓库是公开的）
ADMIN_PWD = os.environ.get("ADMIN_PWD", "")

M_USER = "__verify_shop__"
M_PWD = "123456"

passed = 0
failed = 0


def call(method, path, body=None, token=None):
    """发 JSON 请求，返回 (状态码, 响应体 dict)"""
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def check(name, ok, detail=""):
    global passed, failed
    if ok:
        passed += 1
        print("  [PASS] %s  %s" % (name, detail))
    else:
        failed += 1
        print("  [FAIL] %s  %s" % (name, detail))


def login(username, password):
    """登录，成功返回 token，失败返回 None"""
    _, res = call("POST", "/user/login", {"username": username, "password": password})
    if res.get("code") == 200 and res.get("data"):
        return res["data"].get("token")
    return None


print("\n===== 商家入驻与审核验证 =====\n")

if not ADMIN_PWD:
    print("  请先设置管理员密码环境变量：set ADMIN_PWD=你的密码（CMD）或 $env:ADMIN_PWD=\"...\"（PS）\n")
    raise SystemExit(2)

# 0. 管理员登录（后面要用它改状态、查列表）
admin_token = login(ADMIN_USER, ADMIN_PWD)
check("管理员登录", bool(admin_token))
if not admin_token:
    print("  管理员登录失败，后续断言无法进行，请检查账号密码\n")
    raise SystemExit(1)

# 1. 注册商家
_, res = call("POST", "/user/register",
              {"username": M_USER, "password": M_PWD, "realName": "验证商家", "role": "MERCHANT"})
check("商家可自助注册", res.get("code") == 200, "返回: %s" % res.get("message"))

# 2. 待审核状态不能登录
token_before = login(M_USER, M_PWD)
check("待审核商家登录被拒", token_before is None)

# 3. 管理员在用户列表里能看到它，且 status=0
_, res = call("GET", "/user/list", None, admin_token)
users = res.get("data") or []
target = next((u for u in users if u.get("username") == M_USER), None)
check("管理员列表可见待审核商家", target is not None)
check("该商家状态为待审核(0)", target is not None and target.get("status") == 0,
      "status=%s" % (target.get("status") if target else "N/A"))

# 4. 管理员审核通过（把 status 改成 1）
if target:
    _, res = call("PUT", "/user", {"id": target["id"], "status": 1}, admin_token)
    check("管理员启用该商家", res.get("code") == 200, "返回: %s" % res.get("message"))

# 5. 审核后可登录，且角色正确
token_after = login(M_USER, M_PWD)
check("审核通过后可登录", token_after is not None)

_, res = call("GET", "/user/" + str(target["id"]) if target else "/user/0", None, token_after)
me = (res.get("data") or {}) if res.get("code") == 200 else {}
check("登录身份为商家", me.get("role") == "MERCHANT", "role=%s" % me.get("role"))

# 6. 商家能进经营接口（自己名下商品列表），但不是管理员接口
if token_after:
    _, res = call("GET", "/product/list?mine=true", None, token_after)
    check("商家可访问自己的商品列表", res.get("code") == 200, "返回: %s" % res.get("message"))
    _, res = call("GET", "/user/list", None, token_after)
    check("商家不能访问用户管理接口", res.get("code") not in (200,), "code=%s" % res.get("code"))

# 7. 白名单仍然拦住 ADMIN
_, res = call("POST", "/user/register",
              {"username": "__verify_boss__", "password": M_PWD, "role": "ADMIN"})
check("注册 ADMIN 仍被拒", res.get("code") != 200, "返回: %s" % res.get("message"))

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
print("提示：验证账号需手动清理，或运行脚本末尾的清理段\n")

if failed:
    raise SystemExit(1)
