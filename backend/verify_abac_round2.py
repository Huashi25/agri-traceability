# -*- coding: utf-8 -*-
"""
第二轮越权验证：商品改派 + 评价归属 + 溯源批次归属

前置数据（若 ID 变化请改下面三个常量）：
    INSERT INTO product (name, category_id, price, stock, status, origin, merchant_id)
    VALUES ('__VERIFY_P2__', 1, 9.90, 100, 1, '测试', NULL);          -- 得到 pid，例如 18
    INSERT INTO product_review (product_id, user_id, user_name, rating, content, status)
    VALUES (pid, 11, 'hau', 5, '__VERIFY_REVIEW__', 1);                -- 得到 rid，例如 4
    INSERT INTO trace_batch (batch_no, product_id, trace_code, status)
    VALUES ('VB...', pid, 'VC...', 0);                                 -- 得到 bid，例如 22

运行：python verify_abac_round2.py
"""
import json
import sys
import urllib.request
import urllib.error

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8082/api"
TARGET_PRODUCT = 18   # 平台自营商品（merchant_id = NULL）
TARGET_REVIEW = 5     # 指向上面商品的评价
TARGET_BATCH = 22     # 指向上面商品的溯源批次
MERCHANT_ID = 5       # 库里唯一商家 merchant

results = []


def call(method, path, token=None, body=None):
    """发起 HTTP 请求，返回 (http_status, 解析后的 json 或原始文本)"""
    url = BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, safe_json(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, safe_json(e.read().decode("utf-8", errors="replace"))


def safe_json(raw):
    try:
        return json.loads(raw)
    except Exception:
        return raw


def check(name, passed, detail=""):
    results.append((name, passed, detail))
    print(("  [PASS] " if passed else "  [FAIL] ") + name + ("   ← " + detail if detail else ""))


def msg(res):
    return res.get("message") if isinstance(res, dict) else str(res)


def login(username, password):
    _, res = call("POST", "/user/login", body={"username": username, "password": password})
    return res["data"]["token"]


print("=== 登录 ===")
m_token = login("merchant", "123456")
a_token = login("admin", "admin123")
print("  merchant / admin 登录成功\n")

print("=== A. 商品归属改派（管理员） ===")

_, res = call("GET", "/product/list?mine=true", m_token)
before = [p["id"] for p in (res.get("data") or [])]
check("改派前商家列表不含该商品", TARGET_PRODUCT not in before, "商家可见 %d 个" % len(before))

st, res = call("PUT", "/product", a_token, {"id": TARGET_PRODUCT, "merchantId": MERCHANT_ID})
check("管理员改派商品给商家成功", st == 200 and res.get("code") == 200, msg(res))

_, res = call("GET", "/product/list?mine=true", m_token)
after = [p["id"] for p in (res.get("data") or [])]
check("改派后商家列表包含该商品", TARGET_PRODUCT in after, "商家可见 %d 个" % len(after))

st, res = call("PUT", "/product", a_token, {"id": TARGET_PRODUCT, "merchantId": None})
check("管理员把商品改回平台自营成功", st == 200 and res.get("code") == 200, msg(res))

_, res = call("GET", "/product/list?mine=true", m_token)
back = [p["id"] for p in (res.get("data") or [])]
check("改回平台自营后商家列表不含该商品", TARGET_PRODUCT not in back, "商家可见 %d 个" % len(back))

_, res = call("GET", "/product/%d" % TARGET_PRODUCT, a_token)
check("平台自营商品的 merchantId 确实为 null",
      res.get("data", {}).get("merchantId") is None,
      "merchantId=%s" % res.get("data", {}).get("merchantId"))

print("\n=== B. 评价归属 ===")

_, res = call("GET", "/review/list?mine=true", m_token)
m_reviews = [r["id"] for r in (res.get("data") or [])]
check("商家评价列表不含他人商品评价", TARGET_REVIEW not in m_reviews, "商家可见 %d 条" % len(m_reviews))

st, res = call("PUT", "/review", m_token, {"id": TARGET_REVIEW, "status": 0})
check("商家隐藏他人商品评价被拒", st == 200 and res.get("code") == 500, msg(res))

st, res = call("DELETE", "/review/%d" % TARGET_REVIEW, m_token)
check("商家删除他人商品评价被拒", st == 200 and res.get("code") == 500, msg(res))

st, res = call("PUT", "/review", a_token, {"id": TARGET_REVIEW, "status": 1})
check("管理员处理该评价成功", st == 200 and res.get("code") == 200, msg(res))

_, res = call("GET", "/review/list")
guest_reviews = [r["id"] for r in (res.get("data") or [])]
check("游客公开评价列表仍包含该评价", TARGET_REVIEW in guest_reviews, "游客可见 %d 条" % len(guest_reviews))

print("\n=== C. 溯源批次归属 ===")

_, res = call("GET", "/trace/batch/list?mine=true", m_token)
m_batches = [b["id"] for b in (res.get("data") or [])]
check("商家批次列表不含他人商品批次", TARGET_BATCH not in m_batches, "商家可见 %d 条" % len(m_batches))

st, res = call("PUT", "/trace/batch", m_token, {"id": TARGET_BATCH, "status": 1})
check("商家修改他人商品批次被拒", st == 200 and res.get("code") == 500, msg(res))

st, res = call("PUT", "/trace/batch", a_token, {"id": TARGET_BATCH, "status": 0})
check("管理员修改该批次成功", st == 200 and res.get("code") == 200, msg(res))

_, res = call("GET", "/trace/batch/list")
guest_batches = [b["id"] for b in (res.get("data") or [])]
check("游客公开批次列表仍包含该批次", TARGET_BATCH in guest_batches, "游客可见 %d 条" % len(guest_batches))

print("\n=== 汇总 ===")
failed = [r for r in results if not r[1]]
print("  通过 %d / %d" % (len(results) - len(failed), len(results)))
if failed:
    print("  失败项：")
    for n, _, d in failed:
        print("    - %s %s" % (n, d))
    sys.exit(1)
print("  全部通过")
