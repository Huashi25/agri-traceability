# -*- coding: utf-8 -*-
"""
补齐评价：给「有评分但没有任何评价记录」的商品补真实评价

为什么需要它：
    backend/sql 的种子数据给 15 个商品都写了 rating（4.3 ~ 4.8），但 product_review
    表里只插了 3 条（挂在商品 1、2 上），而且插完没有回写 product.review_count。
    结果就是商品详情页会出现「4.5 星 / 0 条评价」这种自相矛盾的展示 —— 答辩时
    评委会直接问「评分为哪来的」。

    本脚本对每个还没有真实评价的商品，都走一遍完整真实流程：
        下单 -> 支付 -> 商家发货 -> 确认收货 -> 提交评价
    最后由 ProductReviewController.recalculateRating 把 rating / review_count
    重算成与评价记录一致的值。

    跑完之后，product.rating = 该商品全部有效评价的平均分，
    product.review_count = 评价条数，两者与 product_review 表严格自洽。

幂等性：
    以「该商品是否已有 status=1 的评价」为准，已有就跳过，可以反复运行。

运行：
    python seed_demo_reviews.py
前置：后端 8080 已启动
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE", "http://127.0.0.1:8080/api")
SQL_FILE = os.environ.get("INIT_SQL", r"E:\agri-traceability\backend\sql\init.sql")

RECEIVER_NAME = "演示收货人"
RECEIVER_PHONE = "13800138000"
RECEIVER_ADDR = "黑龙江省哈尔滨市香坊区和平路 100 号 演示地址"

# 每个商品一条评价：(商品ID, 星级, 评论文案)
# 文案按商品特性写，避免十几条一模一样的“很好，下次再买”，演示时更真实
REVIEW_PLAN = [
    (5,  5, "石榴籽粒饱满、颜色透亮，一掰就开，酸甜比例刚好，家里老人小孩都爱吃。"),
    (6,  5, "苹果脆度高，切开糖心明显，放了半个月也没面，阿克苏这个季节确实顶。"),
    (7,  4, "柚子水分足、不苦，剥起来费点劲但很值，个头一个有四五斤，性价比高。"),
    (8,  5, "章丘大葱名副其实，葱白有一尺多长，生吃都不辣，拌豆腐最合适。"),
    (9,  5, "松茸带着泥土和松针味，切片煎一下就香得不行，冷链到手还是硬的。"),
    (10, 5, "兰州百合瓣大肉厚，煮银耳汤微甜不苦，比超市的干百合强太多。"),
    (11, 4, "稻花香五常米开袋就有米香，煮粥浓稠，就是价格稍高，活动时囤更划算。"),
    (12, 5, "小米颜色金黄，熬出来的粥上面一层米油，胃不好的人喝着特别舒服。"),
    (13, 4, "黑豆颗粒均匀，泡一夜就能煮烂，打豆浆很香，就是包装封口可以再紧一点。"),
    (14, 5, "滩羊腿肉几乎没有膻味，清炖就很好吃，肉纤维细，一公斤分量足。"),
    (15, 5, "小龙虾个头均匀、活力足，处理得干净，蒜蓉一炒家里人都说比店里强。"),
]

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
    """从 init.sql 现场解析种子账号密码，不写死在脚本里"""
    with open(SQL_FILE, encoding="utf-8") as f:
        text = f.read()
    m = re.search(r"\('%s',\s*'([^']+)'" % re.escape(username), text)
    return m.group(1) if m else None


def login(username, password):
    """登录，返回 token"""
    _, res = call("POST", "/user/login", {"username": username, "password": password})
    if res.get("code") != 200:
        return None
    return (res.get("data") or {}).get("token")


def fetch_products():
    """取商品列表，兼容分页对象与纯数组两种返回形态"""
    _, res = call("GET", "/product/list")
    data = res.get("data")
    if isinstance(data, dict):
        return data.get("records") or data.get("list") or []
    return data or []


def reviewed_product_ids():
    """取已有有效评价（status=1）的商品 ID 集合，用于幂等判断"""
    _, res = call("GET", "/review/list")
    rows = res.get("data") or []
    return {r.get("productId") for r in rows if r.get("status") == 1}


def buy_and_review(tok_consumer, tok_merchant, product_id, quantity, rating, content):
    """
    对一个商品走完整闭环：下单 -> 支付 -> 发货 -> 收货 -> 评价。
    任一步失败立即返回错误描述，由调用方决定是否中断。
    """
    # 1. 下单
    _, res = call("POST", "/order", {
        "order": {
            "receiverName": RECEIVER_NAME,
            "receiverPhone": RECEIVER_PHONE,
            "receiverAddress": RECEIVER_ADDR,
            "paymentMethod": 1,
        },
        "items": [{"productId": product_id, "quantity": quantity}],
    }, tok_consumer)
    if res.get("code") != 200:
        return None, "下单失败: %s" % res.get("message")
    order_id = (res.get("data") or {}).get("id")

    # 2. 支付
    _, res = call("PUT", "/order/pay/%d" % order_id, None, tok_consumer)
    if res.get("code") != 200:
        return None, "支付失败: %s" % res.get("message")

    # 3. 商家发货（同时验证商家对自己商品的发货权限）
    _, res = call("PUT", "/order/ship/%d" % order_id, None, tok_merchant)
    if res.get("code") != 200:
        return None, "发货失败: %s" % res.get("message")

    # 4. 消费者确认收货（后端会累加 sales_count）
    _, res = call("PUT", "/order/confirm/%d" % order_id, None, tok_consumer)
    if res.get("code") != 200:
        return None, "收货失败: %s" % res.get("message")

    # 5. 提交评价（后端会重算 rating / review_count）
    _, res = call("POST", "/review", {
        "productId": product_id, "rating": rating, "content": content,
    }, tok_consumer)
    if res.get("code") != 200:
        return None, "评价失败: %s" % res.get("message")

    return order_id, None


print("\n===== 补齐商品评价（有评分无评价 -> 真实评价） =====\n")

pwd_consumer = read_seed_pwd("consumer")
pwd_merchant = read_seed_pwd("merchant")
if not (pwd_consumer and pwd_merchant):
    print("init.sql 里解析不到种子账号密码，检查 SQL_FILE =", SQL_FILE)
    sys.exit(1)

tok_consumer = login("consumer", pwd_consumer)
tok_merchant = login("merchant", pwd_merchant)
check("consumer 登录", bool(tok_consumer))
check("merchant 登录", bool(tok_merchant))
if not (tok_consumer and tok_merchant):
    sys.exit(1)

have_reviews = reviewed_product_ids()
products = fetch_products()
check("商品列表可读", len(products) == 15, "实际 %d 个商品" % len(products))

# 挑出「有评分但零评价」的商品 —— 这就是页面自相矛盾的根源
suspect = [p for p in products if p.get("id") not in have_reviews]
print("\n待补评价商品 %d 个：%s" % (len(suspect), ", ".join(str(p["id"]) for p in suspect)))

plan_map = {pid: (rating, content) for pid, rating, content in REVIEW_PLAN}
print("\n-- 逐商品走完整闭环 --")
for p in suspect:
    pid = p["id"]
    if pid not in plan_map:
        print("  [SKIP] 商品%d 没有预置评论文案，跳过" % pid)
        continue
    rating, content = plan_map[pid]
    order_id, err = buy_and_review(tok_consumer, tok_merchant, pid, 1, rating, content)
    check("商品%d（%s）闭环 + 评价" % (pid, p.get("name")), order_id is not None,
          err if err else "orderId=%s" % order_id)

# ---------- 一致性复核：rating / review_count 必须与评价行严格一致 ----------
print("\n-- 一致性复核 --")
products_after = fetch_products()
inconsistent = []
for p in products_after:
    rc = p.get("reviewCount") or 0
    if rc == 0 and float(p.get("rating") or 0) > 0:
        inconsistent.append("%s(评分%.1f/0条)" % (p.get("name"), float(p.get("rating"))))
check("没有「有评分但0条评价」的商品", not inconsistent,
      "残留: %s" % ", ".join(inconsistent) if inconsistent else "全部自洽")

rev_ids = reviewed_product_ids()
missing_rows = [str(p["id"]) for p in products_after
                if (p.get("reviewCount") or 0) > 0 and p["id"] not in rev_ids]
check("每个有评分的商品都有真实评价记录", not missing_rows,
      "缺记录: %s" % ", ".join(missing_rows) if missing_rows else "全部对得上")

reviewed_count = sum(1 for p in products_after if (p.get("reviewCount") or 0) > 0)
check("已产生评价的商品数 >= 11", reviewed_count >= 11, "实际 %d 个商品有评价" % reviewed_count)

_, res = call("GET", "/admin/stats/overview", None, tok_merchant)
stats = res.get("data") or {}
check("商家看板评价数 > 0", (stats.get("reviewCount") or 0) > 0,
      "reviewCount=%s" % stats.get("reviewCount"))

print("\n===== 结果: %d PASS / %d FAIL =====\n" % (passed, failed))
if failed:
    sys.exit(1)
