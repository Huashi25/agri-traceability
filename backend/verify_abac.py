# -*- coding: utf-8 -*-
"""
商品/订单商家归属隔离 —— 端到端验证脚本（第一轮）

目标：确认 MERCHANT 无法读写不属于自己商品的订单，且商品归属无法被伪造。

前置数据（脚本跑完后请自行清理；ID 变了就改下面三个常量）：
    INSERT INTO product (name, category_id, price, stock, status, origin, merchant_id)
    VALUES ('__VERIFY_PLATFORM_OWNED__', 1, 9.90, 100, 1, '测试', NULL);
    -- 记下 LAST_INSERT_ID() 作为产品 ID

    INSERT INTO order_info (order_no, user_id, total_amount, status, create_time)
    VALUES (CONCAT('VERIFY', UNIX_TIMESTAMP()), 11, 9.90, 0, NOW());
    -- 记下 LAST_INSERT_ID() 作为订单 ID

    INSERT INTO order_item (order_id, product_id, product_name, product_price, quantity, subtotal)
    VALUES (<订单ID>, <产品ID>, '__VERIFY_PLATFORM_OWNED__', 9.90, 1, 9.90);

清理：
    DELETE FROM order_item WHERE order_id = <订单ID>;
    DELETE FROM order_info  WHERE id = <订单ID>;
    DELETE FROM product     WHERE id = <产品ID>;

运行：python verify_abac.py
"""
import json
import sys
import urllib.request
import urllib.error

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8082/api"
VERIFY_ORDER_ID = 8          # 平台自营商品对应的订单（不属于商家 5）
VERIFY_PRODUCT_ID = 16       # 平台自营商品（merchant_id = NULL）

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
            raw = resp.read().decode("utf-8")
            return resp.status, safe_json(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        return e.code, safe_json(raw)


def safe_json(raw):
    try:
        return json.loads(raw)
    except Exception:
        return raw


def check(name, passed, detail=""):
    results.append((name, passed, detail))
    print(("  [PASS] " if passed else "  [FAIL] ") + name + ("   ← " + detail if detail else ""))


def login(username, password):
    _, res = call("POST", "/user/login", body={"username": username, "password": password})
    return res["data"]["token"]


print("=== 登录 ===")
m_token = login("merchant", "123456")
a_token = login("admin", "admin123")
print("  merchant / admin 均登录成功（游客列表用匿名请求验证）\n")

print("=== 订单归属隔离（靶子：订单 %d，含平台自营商品） ===" % VERIFY_ORDER_ID)

# 1. 商家订单列表不应包含平台自营订单
st, res = call("GET", "/admin/order/list", m_token)
ids = [o["id"] for o in (res.get("data") or [])] if isinstance(res, dict) else []
check("商家订单列表不含平台自营订单", VERIFY_ORDER_ID not in ids,
      "商家可见 %d 条，最大 id=%s" % (len(ids), max(ids) if ids else "-"))

# 2. 商家看详情被拒
st, res = call("GET", "/admin/order/%d" % VERIFY_ORDER_ID, m_token)
check("商家查看他人订单详情被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 3. 商家看明细被拒
st, res = call("GET", "/admin/order/items/%d" % VERIFY_ORDER_ID, m_token)
check("商家查看他人订单明细被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 4. 商家发货被拒
st, res = call("PUT", "/admin/order/ship/%d" % VERIFY_ORDER_ID, m_token)
check("商家对他人订单发货被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 5. 商家取消被拒
st, res = call("PUT", "/admin/order/cancel/%d" % VERIFY_ORDER_ID, m_token)
check("商家取消他人订单被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 6. 商家走通用 update 改单被角色拦截（403）
st, res = call("PUT", "/admin/order", m_token, {"id": VERIFY_ORDER_ID, "status": 3})
check("商家调用通用订单更新被 403 拦截", st == 403,
      "HTTP %d" % st)

# 7. 管理员仍可看
st, res = call("GET", "/admin/order/list", a_token)
a_ids = [o["id"] for o in (res.get("data") or [])] if isinstance(res, dict) else []
check("管理员订单列表包含该订单", VERIFY_ORDER_ID in a_ids, "管理员可见 %d 条" % len(a_ids))

# 8. 管理员可看详情
st, res = call("GET", "/admin/order/%d" % VERIFY_ORDER_ID, a_token)
check("管理员查看订单详情正常", st == 200 and res.get("code") == 200 and res.get("data"))

# 9. 管理员仍可改单
st, res = call("PUT", "/admin/order", a_token, {"id": VERIFY_ORDER_ID, "status": 0})
check("管理员调用通用订单更新正常", st == 200 and res.get("code") == 200, "HTTP %d" % st)

# 10. 商家统计里订单数应比管理员少 1
_, m_stats = call("GET", "/admin/order/stats", m_token)
_, a_stats = call("GET", "/admin/order/stats", a_token)
m_total, a_total = m_stats["data"]["total"], a_stats["data"]["total"]
check("商家统计口径与可见范围一致", m_total == a_total - 1,
      "商家 %d 条 / 管理员 %d 条" % (m_total, a_total))

print("\n=== 商品归属隔离 ===")

# 11. 商家改他人商品被拒
st, res = call("PUT", "/product", m_token, {"id": VERIFY_PRODUCT_ID, "name": "HACKED_BY_MERCHANT"})
check("商家修改平台自营商品被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 12. 商家删他人商品被拒
st, res = call("DELETE", "/product/%d" % VERIFY_PRODUCT_ID, m_token)
check("商家删除平台自营商品被拒", st == 200 and res.get("code") == 500,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 13. 商家新增商品时伪造归属，应被强制改回自己
st, res = call("POST", "/product", m_token,
               {"name": "__VERIFY_FAKE_OWNER__", "categoryId": 1, "price": 1.00,
                "stock": 1, "status": 1, "merchantId": 999})
check("商家新增商品请求成功", st == 200 and res.get("code") == 200,
      "返回：%s" % (res.get("message") if isinstance(res, dict) else res))

# 14. 商家后台商品列表只含自己的商品
st, res = call("GET", "/product/list?mine=true", m_token)
m_products = res.get("data") or []
check("商家 mine=true 列表不含平台自营商品",
      all(p["id"] != VERIFY_PRODUCT_ID for p in m_products),
      "商家可见 %d 个商品" % len(m_products))
check("商家 mine=true 列表全部归属自己",
      all(p.get("merchantId") == 5 for p in m_products),
      "归属集合：%s" % sorted({p.get("merchantId") for p in m_products}))

# 15. 消费者公开列表不受影响（匿名访问 /product/list，仍能看到平台自营商品）
st, res = call("GET", "/product/list")
c_products = res.get("data") or []
check("游客公开列表仍包含平台自营商品",
      any(p["id"] == VERIFY_PRODUCT_ID for p in c_products),
      "游客可见 %d 个商品" % len(c_products))

print("\n=== 汇总 ===")
failed = [r for r in results if not r[1]]
print("  通过 %d / %d" % (len(results) - len(failed), len(results)))
if failed:
    print("  失败项：")
    for n, _, d in failed:
        print("    - %s %s" % (n, d))
    sys.exit(1)
print("  全部通过")
