# -*- coding: utf-8 -*-
"""
演示数据种子脚本：把订单 / 评价 / 通知造齐，让后台各页面不再空白

背景：
    2026-10-29 数据库重建后，order_info / order_item / product_review 三张表基本是空的。
    这些属于运行时数据，backend/sql 里的建库脚本本来就不覆盖，所以答辩演示时后台的
    「订单管理」「发货工作台」「数据统计」「评价管理」全是空白页，很难看。

做法（不走 SQL 直插，全程走真实 HTTP 接口）：
    这样造出来的数据本身就是一次全链路回归 —— 下单扣库存、支付、发货、收货、评价
    重算评分，每一步都经过 Controller → Service → Mapper，比 SQL 直插可信得多。

    角色分工（顺手验证权限隔离）：
      consumer  下单 / 支付 / 收货 / 评价
      merchant  发货（验证商家只能发含自己商品的订单）
      admin     发其中一单（验证管理员全平台权限）

生成的 6 笔订单（覆盖全部 5 种状态）：
    A  榴莲×1 + 脐橙×2 + 五常大米×2   → 已完成(3)  → 产出 3 条评价
    B  章丘大葱×2                      → 已发货(2)  → 演示「确认收货」
    C  山东大葱×3                      → 待发货(1)  → 发货工作台主战场
    D  云南松茸×1                      → 待付款(0)  → 演示「支付」按钮
    E  阿克苏苹果×2                    → 已取消(4)  → 演示取消与库存恢复
    F  山东大葱×1                      → 已完成(3)  → 产出第 4 条评价

运行：
    python seed_demo_orders.py
前置：
    后端 8080 已启动、MySQL 已运行（服务 MySQL80）
清理（要重来时）：
    DELETE oi, oit FROM order_info oi LEFT JOIN order_item oit ON oit.order_id = oi.id
      WHERE oi.receiver_name = '演示收货人';
    DELETE FROM product_review WHERE user_name = 'consumer';
    DELETE FROM user_address WHERE receiver_name = '演示收货人';
    DELETE FROM notification WHERE user_id = (SELECT id FROM sys_user WHERE username='consumer');
    注意：清理不会回滚库存 / 销量 / 评分，要彻底复位请重建库后重跑本脚本。
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE", "http://127.0.0.1:8080/api")
SQL_FILE = os.environ.get("INIT_SQL", r"E:\agri-traceability\backend\sql\init.sql")

# 演示收货信息：统一打标记，方便日后一条 SQL 清理
RECEIVER_NAME = "演示收货人"
RECEIVER_PHONE = "13800138000"
RECEIVER_ADDR = "黑龙江省哈尔滨市香坊区和平路 100 号 演示地址"

passed = 0
failed = 0


def call(method, path, body=None, token=None):
    """
    发一个 JSON 请求，返回 (HTTP 状态码, 响应体 dict)。
    本机回环必须绕过系统代理，否则会被 HTTP_PROXY 拦成 502。
    """
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
    except Exception as e:  # 连接层失败（后端没起、端口不通）
        return 0, {"raw": str(e)}


def check(name, ok, detail=""):
    """记录一条断言结果，失败不中断（跑完再统一汇总）"""
    global passed, failed
    if ok:
        passed += 1
        print("  [PASS] %s  %s" % (name, detail))
    else:
        failed += 1
        print("  [FAIL] %s  %s" % (name, detail))
    return ok


def read_seed_pwd(username):
    """
    从 init.sql 里现场解析种子账号的明文密码。
    不把密码写进脚本 —— 仓库是公开的，且脚本会进 git。
    """
    with open(SQL_FILE, encoding="utf-8") as f:
        text = f.read()
    m = re.search(r"\('%s',\s*'([^']+)'" % re.escape(username), text)
    return m.group(1) if m else None


def login(username, password):
    """登录并返回 (token, user)；失败返回 (None, None)"""
    _, res = call("POST", "/user/login", {"username": username, "password": password})
    if res.get("code") != 200:
        return None, None
    data = res.get("data") or {}
    return data.get("token"), data.get("user") or {}


def create_order(token, items, payment_method=1):
    """
    下单。items 形如 [{"productId":1,"quantity":1}, ...]
    后端会忽略前端传的价格与 userId，强制用库里的价格与当前登录人。
    """
    body = {
        "order": {
            "receiverName": RECEIVER_NAME,
            "receiverPhone": RECEIVER_PHONE,
            "receiverAddress": RECEIVER_ADDR,
            "paymentMethod": payment_method,
        },
        "items": [{"productId": i["productId"], "quantity": i["quantity"]} for i in items],
    }
    _, res = call("POST", "/order", body, token)
    order = (res.get("data") or {}) if res.get("code") == 200 else None
    return order, res


def stock_of(product_id, all_products):
    """从商品列表里取某个商品的库存，取不到返回 None"""
    for p in all_products:
        if p.get("id") == product_id:
            return p.get("stock")
    return None


def get_products():
    """拉全量商品列表，兼容后端返回分页对象或纯数组两种形态"""
    _, res = call("GET", "/product/list")
    data = res.get("data")
    if isinstance(data, dict):
        return data.get("records") or data.get("list") or []
    return data or []


print("\n===== 演示数据种子（订单 / 评价 / 通知） =====\n")

# ---------- 0. 前置检查：后端是否活着、是否已有演示数据 ----------
_, ping = call("GET", "/category/list")
if ping.get("code") != 200:
    print("后端不可用，先启动 8080：", ping)
    sys.exit(1)

pwd_consumer = read_seed_pwd("consumer")
pwd_merchant = read_seed_pwd("merchant")
pwd_admin = read_seed_pwd("admin")
if not (pwd_consumer and pwd_merchant and pwd_admin):
    print("init.sql 里解析不到种子账号密码，检查 SQL_FILE =", SQL_FILE)
    sys.exit(1)
check("三个种子账号密码可解析", True, "长度 %d / %d / %d" %
      (len(pwd_consumer), len(pwd_merchant), len(pwd_admin)))

t_consumer, u_consumer = login("consumer", pwd_consumer)
t_merchant, _ = login("merchant", pwd_merchant)
t_admin, _ = login("admin", pwd_admin)
check("consumer 登录", bool(t_consumer), "userId=%s" % (u_consumer or {}).get("id"))
check("merchant 登录", bool(t_merchant))
check("admin 登录", bool(t_admin))
if not (t_consumer and t_merchant and t_admin):
    sys.exit(1)

# 幂等防护：已有演示订单就停手，避免重复下单把库存刷掉
_, res = call("GET", "/order/list", None, t_consumer)
existing = res.get("data") or []
demo_existing = [o for o in existing if o.get("receiverName") == RECEIVER_NAME]
if demo_existing:
    print("\n检测到 %d 笔已存在的演示订单，脚本不再重复下单。" % len(demo_existing))
    print("要重来请先按文件头的「清理」SQL 删除，再重建库级复位。")
    sys.exit(0)

# ---------- 1. 收货地址（让「地址管理」页面不空） ----------
_, res = call("POST", "/address", {
    "receiverName": RECEIVER_NAME,
    "receiverPhone": RECEIVER_PHONE,
    "receiverAddress": RECEIVER_ADDR,
    "isDefault": 1,
}, t_consumer)
check("新增收货地址", res.get("code") == 200, "返回: %s" % res.get("message"))

# ---------- 2. 记录下单前库存，用于验证扣减 ----------
products_before = get_products()
stock_before = {p["id"]: p.get("stock") for p in products_before}
sales_before = {p["id"]: p.get("salesCount") for p in products_before}

# ---------- 3. 下单 6 笔 ----------
print("\n-- 下单 --")
orders = {}

order_a, res = create_order(t_consumer, [
    {"productId": 1, "quantity": 1},   # 海南金枕榴莲 89.90
    {"productId": 2, "quantity": 2},   # 赣南脐橙 39.90
    {"productId": 3, "quantity": 2},   # 五常大米 59.90
])
check("订单A 创建（3 件商品）", order_a is not None, "返回: %s" % res.get("message"))
if order_a:
    orders["A"] = order_a["id"]
    # 89.90*1 + 39.90*2 + 59.90*2 = 289.50，验证后端按库价重算而不是信任前端
    expect = 89.90 + 39.90 * 2 + 59.90 * 2
    check("订单A 金额按库价重算", abs(float(order_a["totalAmount"]) - expect) < 0.01,
          "实际 %.2f / 预期 %.2f" % (float(order_a["totalAmount"]), expect))
    check("订单A 订单号格式", str(order_a.get("orderNo", "")).startswith("ORD"),
          "orderNo=%s" % order_a.get("orderNo"))

order_b, res = create_order(t_consumer, [{"productId": 8, "quantity": 2}])   # 章丘大葱
check("订单B 创建（章丘大葱×2）", order_b is not None, "返回: %s" % res.get("message"))
if order_b:
    orders["B"] = order_b["id"]

order_c, res = create_order(t_consumer, [{"productId": 4, "quantity": 3}])   # 山东大葱
check("订单C 创建（山东大葱×3）", order_c is not None, "返回: %s" % res.get("message"))
if order_c:
    orders["C"] = order_c["id"]

order_d, res = create_order(t_consumer, [{"productId": 9, "quantity": 1}])   # 云南松茸
check("订单D 创建（云南松茸×1）", order_d is not None, "返回: %s" % res.get("message"))
if order_d:
    orders["D"] = order_d["id"]

order_e, res = create_order(t_consumer, [{"productId": 6, "quantity": 2}])   # 阿克苏苹果
check("订单E 创建（阿克苏苹果×2）", order_e is not None, "返回: %s" % res.get("message"))
if order_e:
    orders["E"] = order_e["id"]

order_f, res = create_order(t_consumer, [{"productId": 4, "quantity": 1}])   # 山东大葱
check("订单F 创建（山东大葱×1）", order_f is not None, "返回: %s" % res.get("message"))
if order_f:
    orders["F"] = order_f["id"]

# 库存扣减校验：榴莲 100 -> 99，脐橙 200 -> 198
products_after_create = get_products()
check("下单后榴莲库存 -1",
      stock_of(1, products_after_create) == stock_before[1] - 1,
      "%s -> %s" % (stock_before[1], stock_of(1, products_after_create)))
check("下单后脐橙库存 -2",
      stock_of(2, products_after_create) == stock_before[2] - 2,
      "%s -> %s" % (stock_before[2], stock_of(2, products_after_create)))

# ---------- 4. 支付（D 不支付，留着演示待付款） ----------
print("\n-- 支付 --")
for k in ("A", "B", "C", "E", "F"):
    if k not in orders:
        continue
    _, res = call("PUT", "/order/pay/%d" % orders[k], None, t_consumer)
    check("订单%s 支付" % k, res.get("code") == 200, "返回: %s" % res.get("message"))

# 状态不允许重复支付，顺带验证状态机
if "A" in orders:
    _, res = call("PUT", "/order/pay/%d" % orders["A"], None, t_consumer)
    check("已支付订单不能重复支付", res.get("code") != 200, "返回: %s" % res.get("message"))

# ---------- 5. 发货：B/F 由商家发，A 由管理员发 ----------
# 注意：订单 C 故意保持「待发货」，留给后台发货工作台做演示样本，这里绝不能发货。
print("\n-- 发货 --")
for k in ("B", "F"):
    if k not in orders:
        continue
    _, res = call("PUT", "/order/ship/%d" % orders[k], None, t_merchant)
    check("商家为订单%s 发货" % k, res.get("code") == 200, "返回: %s" % res.get("message"))

if "A" in orders:
    _, res = call("PUT", "/order/ship/%d" % orders["A"], None, t_admin)
    check("管理员为订单A 发货", res.get("code") == 200, "返回: %s" % res.get("message"))

# 消费者无权发货（越权防护回归）。用仍在「待发货」的订单 C 试，
# 这样能确认被拦的是角色权限，而不是订单状态。
if "C" in orders:
    _, res = call("PUT", "/order/ship/%d" % orders["C"], None, t_consumer)
    check("消费者不能发货", res.get("code") != 200, "返回: %s" % res.get("message"))

# ---------- 6. 确认收货：A / F ----------
print("\n-- 确认收货 --")
for k in ("A", "F"):
    if k not in orders:
        continue
    _, res = call("PUT", "/order/confirm/%d" % orders[k], None, t_consumer)
    check("订单%s 确认收货" % k, res.get("code") == 200, "返回: %s" % res.get("message"))

# 未发货的订单不能收货
if "D" in orders:
    _, res = call("PUT", "/order/confirm/%d" % orders["D"], None, t_consumer)
    check("未发货订单不能确认收货", res.get("code") != 200, "返回: %s" % res.get("message"))

# ---------- 7. 取消 E（验证库存恢复） ----------
print("\n-- 取消 --")
if "E" in orders:
    apple_stock_paid = stock_of(6, get_products())
    _, res = call("PUT", "/order/cancel/%d" % orders["E"], None, t_consumer)
    check("订单E 取消", res.get("code") == 200, "返回: %s" % res.get("message"))
    check("取消后苹果库存恢复 +2",
          stock_of(6, get_products()) == apple_stock_paid + 2,
          "%s -> %s" % (apple_stock_paid, stock_of(6, get_products())))

# ---------- 8. 评价（只有已完成订单可评） ----------
print("\n-- 评价 --")
REVIEWS = [
    (1, 5, "榴莲到货就能闻到香味，果肉厚实不空包，冷链做得很好，路上两天一点没坏。"),
    (2, 4, "橙子酸甜适口，皮薄好剥，个头比想象中均匀，就是有两三个稍微小一点，整体很满意。"),
    (3, 5, "米粒饱满，煮出来的饭有回甘，电饭锅一开盖整屋子都是米香，会回购。"),
    (4, 4, "大葱很新鲜，葱白长且紧实，做菜炝锅特别香，包装捆得也整齐。"),
]
for pid, rating, content in REVIEWS:
    _, res = call("POST", "/review", {
        "productId": pid, "rating": rating, "content": content,
    }, t_consumer)
    check("商品%d 评价 %d 星" % (pid, rating), res.get("code") == 200,
          "返回: %s" % res.get("message"))

# 重复评价应被拦
_, res = call("POST", "/review", {"productId": 1, "rating": 5, "content": "重复评价测试"}, t_consumer)
check("同一商品不能重复评价", res.get("code") != 200, "返回: %s" % res.get("message"))

# 未购买的商品不能评价
_, res = call("POST", "/review", {"productId": 15, "rating": 5, "content": "没买过也来评"}, t_consumer)
check("未购买商品不能评价", res.get("code") != 200, "返回: %s" % res.get("message"))

# ---------- 9. 结果校验 ----------
print("\n-- 结果校验 --")
_, res = call("GET", "/order/list", None, t_consumer)
orders_now = res.get("data") or []
status_map = {}
for o in orders_now:
    status_map.setdefault(o.get("status"), 0)
    status_map[o["status"]] += 1
check("订单总数为 6", len(orders_now) == 6, "实际 %d 笔" % len(orders_now))
check("五种状态全覆盖",
      set(status_map.keys()) == {0, 1, 2, 3, 4},
      "状态分布 %s" % json.dumps(status_map, ensure_ascii=False))

products_now = get_products()
p1 = next((p for p in products_now if p["id"] == 1), {})
# 商品 1 在 init.sql 里本来就带着 2 条种子评价（testuser / customer1），
# 所以不能断言「评价数 == 1」。这里只校验 recalculateRating 确实跑过并自洽：
# 评分落在 1~5 之间，且评价数不少于 1 条。
check("商品1 评分已重算（1~5 之间）", 1 <= float(p1.get("rating") or 0) <= 5,
      "rating=%s" % p1.get("rating"))
check("商品1 评价数 >= 1", (p1.get("reviewCount") or 0) >= 1,
      "reviewCount=%s" % p1.get("reviewCount"))
check("商品1 销量 +1",
      p1.get("salesCount") == (sales_before.get(1) or 0) + 1,
      "%s -> %s" % (sales_before.get(1), p1.get("salesCount")))

_, res = call("GET", "/review/list")
reviews_now = res.get("data") or []
check("评价列表至少 4 条", len(reviews_now) >= 4, "实际 %d 条" % len(reviews_now))

_, res = call("GET", "/admin/stats/overview", None, t_admin)
stats = res.get("data") or {}
check("后台统计订单数 > 0", (stats.get("orderCount") or 0) > 0,
      "orderCount=%s" % stats.get("orderCount"))
print("       统计快照: %s" % json.dumps(stats, ensure_ascii=False)[:260])

_, res = call("GET", "/admin/order/pending-ship", None, t_admin)
pending = res.get("data") or []
check("发货工作台有待发货订单", len(pending) >= 1, "待发货 %d 笔" % len(pending))

# 商家看自己的订单（归属隔离回归）
_, res = call("GET", "/admin/order/list", None, t_merchant)
m_orders = res.get("data") or []
check("商家可见订单（含自己商品）", len(m_orders) >= 6, "商家可见 %d 笔" % len(m_orders))

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
if failed:
    sys.exit(1)
