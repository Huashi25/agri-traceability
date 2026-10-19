# -*- coding: utf-8 -*-
"""
批量发货 / 待发货工作台 —— 端到端验证（第四轮补充）

为什么单独一个脚本：
    库里当前没有"已付款待发货"的订单，verify_abac_round3.py 里的
    批量发货断言会被跳过。而批量发货恰恰是这一轮新功能里风险最高的
    （能一次改多单状态），必须用真实数据验证归属校验没被绕过。

做法：
    脚本自己建两笔 status=1 的靶子订单 ——
       A. 平台自营商品（merchant_id=NULL）的订单  → 商家不该看到、更不该能发
       B. 商家自己商品（id=1）的订单              → 商家应该看到并能发
    验证完立即删除，不影响现有数据。

运行：
    python verify_batch_ship.py
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8082/api"
MYSQL = r"D:/MySQL/MySQL Server 8.0/bin/mysql.exe"
DB_ARGS = ["-uroot", "-proot", "-h127.0.0.1", "-P3306",
           "agri_traceability", "--default-character-set=utf8mb4"]

results = []


def sql(query):
    """执行 SQL，返回标准输出的文本"""
    proc = subprocess.run([MYSQL] + DB_ARGS + ["-N", "-B", "-e", query],
                          capture_output=True)
    return proc.stdout.decode("utf-8", errors="replace").strip()


def call(method, path, token=None, body=None):
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
    return res["data"]["token"]


print("=== 造靶子数据 ===")
create_sql = """
SET NAMES utf8mb4;
INSERT INTO product (name, category_id, price, stock, status, origin, merchant_id)
VALUES ('__VERIFY_SHIP_PLATFORM__', 1, 9.90, 100, 1, '测试', NULL);
SET @pid = LAST_INSERT_ID();

INSERT INTO order_info (order_no, user_id, total_amount, status, receiver_name, receiver_phone, receiver_address, create_time, pay_time)
VALUES (CONCAT('VSHP-P-', UNIX_TIMESTAMP()), 11, 9.90, 1, '测试收件人', '13800000000', '测试地址', NOW(), NOW());
SET @oid_p = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, product_name, product_price, quantity, subtotal)
VALUES (@oid_p, @pid, '__VERIFY_SHIP_PLATFORM__', 9.90, 1, 9.90);

INSERT INTO order_info (order_no, user_id, total_amount, status, receiver_name, receiver_phone, receiver_address, create_time, pay_time)
VALUES (CONCAT('VSHP-M-', UNIX_TIMESTAMP()), 11, 9.90, 1, '测试收件人', '13800000000', '测试地址', NOW(), NOW());
SET @oid_m = LAST_INSERT_ID();
INSERT INTO order_item (order_id, product_id, product_name, product_price, quantity, subtotal)
VALUES (@oid_m, 1, '海南金枕榴莲', 9.90, 1, 9.90);

SELECT @pid, @oid_p, @oid_m;
"""
row = sql(create_sql).split("\t")
platform_product_id = int(row[0])
platform_order_id = int(row[1])
merchant_order_id = int(row[2])
print("  平台自营商品=%s，平台订单=%s，商家订单=%s"
      % (platform_product_id, platform_order_id, merchant_order_id))

try:
    a_token = login("admin", "admin123")
    m_token = login("merchant", "123456")

    print("\n=== 待发货工作台 ===")
    _, pending = call("GET", "/admin/order/pending-ship", m_token)
    p_orders = [r["order"]["id"] for r in (pending.get("data") or [])]
    check("商家能看到自己商品的待发货订单", merchant_order_id in p_orders,
          "商家待发货 %d 笔" % len(p_orders))
    check("商家看不到平台自营的待发货订单", platform_order_id not in p_orders,
          "平台订单 %s 不在列表内" % platform_order_id)

    _, pending_a = call("GET", "/admin/order/pending-ship", a_token)
    a_orders = [r["order"]["id"] for r in (pending_a.get("data") or [])]
    check("管理员能看到全部待发货订单",
          merchant_order_id in a_orders and platform_order_id in a_orders,
          "管理员待发货 %d 笔" % len(a_orders))

    # 待发货工作台要带明细，否则页面渲染不出商品
    row0 = next((r for r in (pending.get("data") or []) if r["order"]["id"] == merchant_order_id), None)
    check("工作台带上订单明细", bool(row0 and row0.get("items")),
          "明细 %d 条" % len(row0.get("items") or []) if row0 else "未找到该订单")

    print("\n=== 批量发货 ===")
    st, res = call("PUT", "/admin/order/batch-ship", m_token,
                   {"ids": [platform_order_id, merchant_order_id]})
    data = res.get("data") or {}
    check("批量发货部分成功部分被拒",
          data.get("success") == 1 and len(data.get("failed") or []) == 1,
          "success=%s, failed=%s" % (data.get("success"), data.get("failed")))

    st_m = sql("SELECT status FROM order_info WHERE id = %d;" % merchant_order_id)
    check("自己的订单已变成已发货(2)", st_m == "2", "status=%s" % st_m)

    st_p = sql("SELECT status FROM order_info WHERE id = %d;" % platform_order_id)
    check("他人订单状态未被改动", st_p == "1", "status=%s（应保持 1）" % st_p)

finally:
    print("\n=== 清理靶子数据 ===")
    sql("""
    DELETE FROM order_item WHERE order_id IN (%d, %d);
    DELETE FROM order_info WHERE id IN (%d, %d);
    DELETE FROM product WHERE id = %d;
    """ % (platform_order_id, merchant_order_id,
           platform_order_id, merchant_order_id, platform_product_id))
    left = sql("SELECT COUNT(*) FROM product WHERE name LIKE '__VERIFY%%';")
    print("  清理完成，残留测试商品 %s 个" % left)

passed = sum(1 for _, ok, _ in results if ok)
total = len(results)
print("\n" + "=" * 56)
print("结果：%d / %d 通过" % (passed, total))
if passed != total:
    for name, ok, detail in results:
        if not ok:
            print("  - %s   (%s)" % (name, detail))
    sys.exit(1)
print("全部通过")
