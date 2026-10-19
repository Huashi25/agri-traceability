# -*- coding: utf-8 -*-
"""
注册接口角色白名单 —— 端到端验证脚本

背景：/api/user/register 是公开接口（不需要登录），修复前会把请求体里的 role 直接写库，
      任何人构造 {"role":"ADMIN"} 就能注册出管理员账号。
本脚本验证白名单是否生效：ADMIN / MERCHANT 必须被拒，CONSUMER / FARMER 正常放行。

运行：python verify_register_role.py
前置：后端已启动（默认连 8082，可用环境变量 BASE 覆盖）
"""

import json
import os
import urllib.request
import urllib.error

BASE = os.environ.get("BASE", "http://localhost:8082/api")
passed = 0
failed = 0


def call(method, path, body=None, token=None):
    """发一个 JSON 请求，返回 (HTTP状态码, 解析后的响应体)"""
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


def try_register(username, role, password="123456"):
    """尝试注册，返回 (是否成功, 提示信息)"""
    body = {"username": username, "password": password, "realName": "验证用", "role": role}
    status, res = call("POST", "/user/register", body)
    ok = bool(res.get("code") == 200)
    return ok, res.get("message") or res.get("msg") or ""


print("\n===== 注册角色白名单验证 =====\n")

# 1~3：伪造高权限角色必须被拒
ok, msg = try_register("__verify_admin__", "ADMIN")
check("伪造 ADMIN 注册被拒", not ok, "返回: %s" % msg)

ok, msg = try_register("__verify_merchant__", "MERCHANT")
check("伪造 MERCHANT 注册被拒", not ok, "返回: %s" % msg)

ok, msg = try_register("__verify_lower__", "admin")
check("小写 admin 注册被拒", not ok, "返回: %s" % msg)

# 4~5：合法角色放行
ok, msg = try_register("__verify_consumer__", "CONSUMER")
check("正常注册 CONSUMER 成功", ok, "返回: %s" % msg)

ok, msg = try_register("__verify_farmer__", "FARMER")
check("正常注册 FARMER 成功", ok, "返回: %s" % msg)

# 6：不传角色默认成普通客户
ok, msg = try_register("__verify_norole__", None)
check("不传角色可注册", ok, "返回: %s" % msg)

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
if failed:
    raise SystemExit(1)
