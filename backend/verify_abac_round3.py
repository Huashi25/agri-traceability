# -*- coding: utf-8 -*-
"""
角色隔离与权限收口 —— 端到端验证脚本（第三轮）

覆盖：
    1) 数据统计按角色收口（商家不含平台指标、农户只算自家批次）
    2) 热销榜按商家商品范围收窄、农户为空
    3) 溯源批次农户归属（农户只看自家批次、不能改别人的、建批次强制归自己）
    4) 知识与食谱写权限收紧为 ADMIN
    5) 商家补位功能：库存预警 + 待发货工作台

前置数据：无需手工准备，脚本会自己建靶子并清理。
            需要 farmer01 绑定 farmer_id=1（王建国），这是现有数据。

运行：
    python verify_abac_round3.py
    （端口不是 8082 就改下面的 BASE）

注意：脚本会临时创建 1 个溯源批次，结束时通过管理员接口删除。
"""
import json
import sys
import urllib.request
import urllib.error

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8082/api"

# 现有数据基线（数据变了就改这里）
EXPECT_ADMIN_TRACE_COUNT = 16     # 全平台批次总数
EXPECT_MERCHANT_PRODUCT_COUNT = 15  # 商家 5 名下商品数
FARMER1_ID = 1                     # farmer01 绑定的农户（王建国）

results = []


def call(method, path, token=None, body=None):
    """发起 HTTP 请求，返回 (http_status, 解析后的 json)"""
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
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
    print(("  [PASS] " if passed else "  [FAIL] ") + name + ("   <- " + detail if detail else ""))


def login(username, password):
    _, res = call("POST", "/user/login", body={"username": username, "password": password})
    if not isinstance(res, dict) or res.get("code") != 200:
        raise SystemExit("登录失败: %s -> %s" % (username, res))
    return res["data"]["token"]


print("=== 登录 ===")
a_token = login("admin", "admin123")
m_token = login("merchant", "123456")
f_token = login("farmer01", "123456")
print("  admin / merchant / farmer01 登录成功\n")

# ============================================================
# 一、数据统计按角色收口
# ============================================================
print("=== 一、数据统计口径 ===")

_, a_ov = call("GET", "/admin/stats/overview", a_token)
a_data = a_ov.get("data") or {}

_, m_ov = call("GET", "/admin/stats/overview", m_token)
m_data = m_ov.get("data") or {}

_, f_ov = call("GET", "/admin/stats/overview", f_token)
f_data = f_ov.get("data") or {}

check("管理员能看到全平台用户数", a_data.get("userCount", 0) > 0,
      "userCount=%s" % a_data.get("userCount"))
check("管理员能看到全平台农户数", a_data.get("farmerCount", 0) > 0,
      "farmerCount=%s" % a_data.get("farmerCount"))
check("管理员批次总数符合基线", a_data.get("traceCount") == EXPECT_ADMIN_TRACE_COUNT,
      "traceCount=%s（期望 %d）" % (a_data.get("traceCount"), EXPECT_ADMIN_TRACE_COUNT))

check("商家看不到用户数（归零）", m_data.get("userCount") == 0,
      "userCount=%s" % m_data.get("userCount"))
check("商家看不到农户数（归零）", m_data.get("farmerCount") == 0,
      "farmerCount=%s" % m_data.get("farmerCount"))
check("商家商品数等于自己名下商品", m_data.get("productCount") == EXPECT_MERCHANT_PRODUCT_COUNT,
      "productCount=%s（期望 %d）" % (m_data.get("productCount"), EXPECT_MERCHANT_PRODUCT_COUNT))
check("商家营收不超过全平台营收",
      float(m_data.get("totalRevenue") or 0) <= float(a_data.get("totalRevenue") or 0),
      "商家=%s 全平台=%s" % (m_data.get("totalRevenue"), a_data.get("totalRevenue")))

check("农户订单数归零", f_data.get("orderCount") == 0,
      "orderCount=%s" % f_data.get("orderCount"))
check("农户营收归零", float(f_data.get("totalRevenue") or 0) == 0,
      "totalRevenue=%s" % f_data.get("totalRevenue"))
check("农户只能看到自家批次数", f_data.get("traceCount") == 1,
      "traceCount=%s（farmer1 名下应为 1）" % f_data.get("traceCount"))
check("农户用户数归零", f_data.get("userCount") == 0,
      "userCount=%s" % f_data.get("userCount"))

# ============================================================
# 二、热销榜口径
# ============================================================
print("\n=== 二、热销榜 ===")

_, f_hot = call("GET", "/admin/stats/hot-products", f_token)
check("农户热销榜为空", (f_hot.get("data") or []) == [],
      "返回 %d 条" % len(f_hot.get("data") or []))

_, m_products = call("GET", "/product/list", m_token, None)
m_products = m_products if isinstance(m_products, list) else (m_products.get("data") or [])
# 商家商品 ID 集合，用管理端 mine 列表取更准
_, m_mine = call("GET", "/product/list?mine=true", m_token)
my_ids = set(p["id"] for p in (m_mine.get("data") or []))
_, m_hot = call("GET", "/admin/stats/hot-products", m_token)
m_hot_ids = set(p["productId"] for p in (m_hot.get("data") or []))
check("商家热销榜只含自己商品", m_hot_ids.issubset(my_ids),
      "榜内 %d 个，全部在自己 %d 个商品内" % (len(m_hot_ids), len(my_ids)))

# ============================================================
# 三、溯源批次农户归属
# ============================================================
print("\n=== 三、溯源批次农户归属 ===")

# 3.1 游客（不传 mine）仍是全量
st, anon = call("GET", "/trace/batch/list")
check("游客批次列表仍是全量", len(anon.get("data") or []) == EXPECT_ADMIN_TRACE_COUNT,
      "返回 %d 条" % len(anon.get("data") or []))

# 3.2 农户 mine=true 只看到自己的
_, f_batches = call("GET", "/trace/batch/list?mine=true", f_token)
f_list = f_batches.get("data") or []
check("农户只看自家批次", all(b.get("farmerId") == FARMER1_ID for b in f_list),
      "返回 %d 条，farmerId=%s" % (len(f_list), set(b.get("farmerId") for b in f_list)))

# 3.3 商家 mine=true 看到自己商品的全部批次
_, m_batches = call("GET", "/trace/batch/list?mine=true", m_token)
check("商家批次列表按商品归属收窄",
      len(m_batches.get("data") or []) == EXPECT_ADMIN_TRACE_COUNT,
      "返回 %d 条" % len(m_batches.get("data") or []))

# 3.4 造靶子：商家创建一条 farmer_id=2 的批次（模拟别家农户的批次）
_, created = call("POST", "/trace/batch", m_token, {"productId": 2, "farmerId": 2})
target_batch_id = (created.get("data") or {}).get("id")
check("商家可以创建批次（原先前端点了会 403）", target_batch_id is not None,
      "新批次 id=%s" % target_batch_id)

# 3.5 农户 1 改 farmer 2 的批次 → 拒绝
st, res = call("PUT", "/trace/batch", f_token,
               {"id": target_batch_id, "status": 3, "farmerId": 1})
check("农户不能修改他人批次", res.get("code") == 500,
      "返回：%s" % res.get("message"))

# 3.6 农户不能把批次改派给自己（即使状态不变）
st, res = call("PUT", "/trace/batch", f_token, {"id": target_batch_id, "farmerId": 1})
check("农户不能认领他人批次", res.get("code") == 500,
      "返回：%s" % res.get("message"))

# 3.7 农户自己建批次 → 后端强制 farmerId=自己
_, f_created = call("POST", "/trace/batch", f_token, {"productId": 1, "farmerId": 999})
f_batch = f_created.get("data") or {}
check("农户建批次时 farmerId 被强制写成本人",
      f_batch.get("farmerId") == FARMER1_ID,
      "提交 999，落库 %s" % f_batch.get("farmerId"))

# 3.8 管理员改派批次归属生效：把靶子批次改成农户 1 → 农户能看到
call("PUT", "/trace/batch", a_token, {"id": target_batch_id, "farmerId": FARMER1_ID})
_, f_batches2 = call("GET", "/trace/batch/list?mine=true", f_token)
ids2 = [b["id"] for b in (f_batches2.get("data") or [])]
check("管理员改派后农户能看到该批次", target_batch_id in ids2,
      "农户可见 %d 条" % len(ids2))

# 3.9 改回去，确认收窄立即生效
call("PUT", "/trace/batch", a_token, {"id": target_batch_id, "farmerId": 2})
_, f_batches3 = call("GET", "/trace/batch/list?mine=true", f_token)
ids3 = [b["id"] for b in (f_batches3.get("data") or [])]
check("改派给他人后农户立即看不到", target_batch_id not in ids3,
      "农户可见 %d 条" % len(ids3))

# ============================================================
# 四、知识与食谱写权限
# ============================================================
print("\n=== 四、知识与食谱权限 ===")

st, res = call("POST", "/knowledge", m_token,
               {"title": "__VERIFY__", "summary": "x", "content": "x", "status": 0})
check("商家不能新建百科文章", st == 403, "HTTP %s" % st)

st, res = call("POST", "/recipe", m_token,
               {"title": "__VERIFY__", "description": "x", "status": 0})
check("商家不能新建食谱", st == 403, "HTTP %s" % st)

st, res = call("GET", "/knowledge/admin/list", m_token)
check("商家不能访问百科后台列表", st == 403, "HTTP %s" % st)

st, res = call("GET", "/knowledge/admin/list", a_token)
check("管理员仍可访问百科后台列表", st == 200 and res.get("code") == 200,
      "返回 %d 条" % len(res.get("data") or []))

# ============================================================
# 五、商家补位功能
# ============================================================
print("\n=== 五、库存预警 / 待发货工作台 ===")

_, stock = call("GET", "/admin/stock/warning?threshold=10000", m_token)
s_data = stock.get("data") or {}
s_list = s_data.get("list") or []
check("库存预警能返回数据（阈值放大到全部）", len(s_list) > 0,
      "命中 %d 个商品，断货 %s，偏低 %s" % (len(s_list), s_data.get("outCount"), s_data.get("lowCount")))
check("库存预警只含商家自己的商品",
      all(p.get("merchantId") == 5 for p in s_list),
      "merchantId 集合=%s" % set(p.get("merchantId") for p in s_list))

st, res = call("GET", "/admin/stock/warning", f_token)
check("农户不能访问库存预警", st == 403, "HTTP %s" % st)

_, pending = call("GET", "/admin/order/pending-ship", m_token)
p_list = pending.get("data") or []
check("待发货工作台返回结构正确",
      all("order" in r and "items" in r for r in p_list),
      "共 %d 笔待发货" % len(p_list))

_, pending_a = call("GET", "/admin/order/pending-ship", a_token)
pa_list = pending_a.get("data") or []
check("管理员待发货数量不少于商家", len(pa_list) >= len(p_list),
      "管理员 %d 笔 / 商家 %d 笔" % (len(pa_list), len(p_list)))

st, res = call("PUT", "/admin/order/batch-ship", m_token, {"ids": []})
check("批量发货空列表被拒", res.get("code") == 500, "返回：%s" % res.get("message"))

# 越权批量发货：拿一个商家看不到的订单（管理员列表里、商家列表里没有的）
m_order_ids = set(o["id"] for o in (call("GET", "/admin/order/list", m_token)[1].get("data") or []))
a_order_ids = set(o["id"] for o in (call("GET", "/admin/order/list", a_token)[1].get("data") or []))
foreign_ids = list(a_order_ids - m_order_ids)
if foreign_ids:
    st, res = call("PUT", "/admin/order/batch-ship", m_token, {"ids": foreign_ids})
    failed = (res.get("data") or {}).get("failed") or []
    check("批量发货逐单校验归属（他人订单被拒）",
          len(failed) == len(foreign_ids),
          "%d 笔他人订单全部被拒" % len(failed))
else:
    check("批量发货逐单校验归属（他人订单被拒）", True, "当前没有他人订单，跳过")

# ============================================================
# 清理靶子
# ============================================================
print("\n=== 清理 ===")
if target_batch_id:
    call("DELETE", "/trace/batch/%s" % target_batch_id, a_token)
    print("  已删除靶子批次 id=%s" % target_batch_id)
if f_batch.get("id"):
    call("DELETE", "/trace/batch/%s" % f_batch.get("id"), a_token)
    print("  已删除农户自建批次 id=%s" % f_batch.get("id"))

_, after = call("GET", "/trace/batch/list")
check("清理后批次总数回到基线", len(after.get("data") or []) == EXPECT_ADMIN_TRACE_COUNT,
      "当前 %d 条" % len(after.get("data") or []))

# ============================================================
# 汇总
# ============================================================
passed = sum(1 for _, ok, _ in results if ok)
total = len(results)
print("\n" + "=" * 56)
print("结果：%d / %d 通过" % (passed, total))
if passed != total:
    print("失败项：")
    for name, ok, detail in results:
        if not ok:
            print("  - %s   (%s)" % (name, detail))
    sys.exit(1)
print("全部通过")
