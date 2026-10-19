# -*- coding: utf-8 -*-
"""
注册不再强制填写资料 —— 端到端验证脚本

背景：
    注册页原先要求填「真实姓名」「手机号」。业务上这两项并非账号必需，
    改为只收 用户名 + 密码 + 确认密码 + 身份，资料放到个人中心按需补充。
    对应改动：Login.vue（表单与校验）、Profile.vue（编辑弹窗回填）。

验证链路：
    1. 注册请求只带 username/password/role → 成功，realName/phone 落库为 NULL
    2. 空资料账号能正常登录
    3. 模拟个人中心「编辑资料」，补填 realName/phone → 保存成功
    4. 补填后资料读得到；且因为保存时没传 password，原密码不受影响（还能登录）
    5. 确认密码只是前端字段，后端不会因为缺少它而报错（本脚本全程不传）
清理：
    脚本结束前删除自己造的账号（需要本机 mysql 客户端可用）。

运行：python verify_register_optional.py
前置：后端已启动（默认 8080，可用环境变量 BASE 覆盖）
"""

import json
import os
import subprocess
import urllib.request
import urllib.error

BASE = os.environ.get("BASE", "http://localhost:8080/api")
U_NAME = "__verify_opt__"
U_PWD = "Opt123456"

# 清理用数据库参数（与 application.yml 默认值一致，可用环境变量覆盖）
DB = os.environ.get("DB_NAME", "agri_traceability")
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PWD = os.environ.get("DB_PASSWORD", "root")

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
    """记录一条断言结果"""
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


def find_mysql():
    """
    定位 mysql 客户端可执行文件。
    本机客户端不一定在 PATH 里（实测装在 D:\\MySQL\\MySQL Server 8.0\\bin），
    所以先查 PATH，再逐个试常见安装目录。
    """
    import shutil
    hit = shutil.which("mysql")
    if hit:
        return hit
    candidates = [
        r"D:\MySQL\MySQL Server 8.0\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        r"C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe",
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    return None


def cleanup():
    """
    删除本脚本造的账号。
    用 mysql 客户端直连（用户名纯 ASCII，不受 Windows 客户端 GBK 影响）。
    失败不抛异常，只提示手动清理。
    """
    sql = "DELETE FROM sys_user WHERE username='%s';" % U_NAME
    exe = find_mysql()
    if not exe:
        print("  未找到 mysql 客户端，请手动执行: %s" % sql)
        return
    try:
        subprocess.run(
            [exe, "-u" + DB_USER, "-p" + DB_PWD, DB, "-e", sql],
            capture_output=True, timeout=20, check=False
        )
        print("  已清理验证账号 %s" % U_NAME)
    except Exception as e:
        print("  清理失败，请手动执行: %s  (%s)" % (sql, e))


print("\n===== 注册资料非必填验证 =====\n")

# 0. 先清一次，避免上次中断留下的同名账号干扰「注册成功」断言
cleanup()

# 1. 注册：只传账号信息，不传 realName / phone / email
_, res = call("POST", "/user/register",
              {"username": U_NAME, "password": U_PWD, "role": "CONSUMER"})
check("不填资料也能注册成功", res.get("code") == 200, "返回: %s" % res.get("message"))

# 2. 空资料账号能登录
token = login(U_NAME, U_PWD)
check("空资料账号可登录", bool(token))

# 3. 读出用户信息，确认资料字段是空
me = {}
uid = None
if token:
    # 用登录返回体里的 user 取 id：注册接口只回 Result<Void>，拿不到 id
    _, res = call("POST", "/user/login", {"username": U_NAME, "password": U_PWD})
    me = (res.get("data") or {}).get("user") or {}
    uid = me.get("id")
check("realName 落库为空", me.get("realName") in (None, ""), "realName=%r" % me.get("realName"))
check("phone 落库为空", me.get("phone") in (None, ""), "phone=%r" % me.get("phone"))

# 4. 模拟个人中心「编辑资料」：只提交资料三项，不传 password
if uid:
    _, res = call("PUT", "/user",
                  {"id": uid, "realName": "验证补填", "phone": "13900009999"},
                  token)
    check("个人中心可补填资料", res.get("code") == 200, "返回: %s" % res.get("message"))

    _, res = call("GET", "/user/%s" % uid, None, token)
    saved = (res.get("data") or {}) if res.get("code") == 200 else {}
    check("补填的姓名已保存", saved.get("realName") == "验证补填", "realName=%r" % saved.get("realName"))
    check("补填的手机号已保存", saved.get("phone") == "13900009999", "phone=%r" % saved.get("phone"))

    # 5. 回归点：保存资料时没传密码，不能把密码清空
    check("补填资料后密码未被清空", bool(login(U_NAME, U_PWD)))

# 6. 清理
cleanup()

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))

if failed:
    raise SystemExit(1)
