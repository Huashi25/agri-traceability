-- ============================================================
-- 商品-商家归属迁移脚本
-- 文件：backend/sql/product_merchant.sql
-- 创建：2026-10-19
-- ------------------------------------------------------------
-- 目的：
--   product 表原本没有任何归属字段，后台商家（role=MERCHANT）登录后
--   能看到并操作全平台商品与订单，属于横向越权。
--   本脚本给商品补上"归属商家"，作为订单/商品越权校验的数据基础。
--
-- 约定：
--   merchant_id = NULL  表示"平台自营"，只有 ADMIN 可管理
--   merchant_id = 某个 MERCHANT 账号的 sys_user.id，表示归该商家
--
-- 执行方式（Windows，注意路径带空格）：
--   "D:/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p agri_traceability < product_merchant.sql
--
-- 回滚方式：
--   若执行前备份过表，可直接
--     ALTER TABLE product DROP COLUMN merchant_id;
--   并从 product_bak_<日期> 恢复数据。
-- ============================================================

USE agri_traceability;

-- ------------------------------------------------------------
-- 0. 执行前备份（强烈建议先执行这一句，按当天日期改名）
-- ------------------------------------------------------------
-- CREATE TABLE product_bak_20261019 AS SELECT * FROM product;

-- ------------------------------------------------------------
-- 1. 新增归属字段
--    放在 category_id 之后，便于按"分类 + 商家"查看
--    MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--    若已加过会报 1060 Duplicate column name，忽略即可
-- ------------------------------------------------------------
ALTER TABLE product
    ADD COLUMN merchant_id BIGINT DEFAULT NULL
    COMMENT '归属商家ID(关联sys_user.id, role=MERCHANT)，NULL=平台自营'
    AFTER category_id;

-- ------------------------------------------------------------
-- 2. 新增索引
--    商家后台按 merchant_id 过滤商品、按商品反查订单，必须走索引
-- ------------------------------------------------------------
ALTER TABLE product ADD KEY idx_merchant (merchant_id);

-- ------------------------------------------------------------
-- 3. 历史数据回填
--    把现有商品统一归给默认商家账号 username='merchant'。
--    这样迁移完成后，商家看到的商品/订单与迁移前完全一致，
--    不会出现"升级后商家一条数据都看不到"的功能退化。
--
--    如果你有多个商家，请跳过本段，改为逐条指定，例如：
--      UPDATE product SET merchant_id = 5  WHERE id IN (1,2,3);
--      UPDATE product SET merchant_id = 8  WHERE id IN (4,5,6);
--    平台自营的商品请保持 merchant_id = NULL。
-- ------------------------------------------------------------
UPDATE product
SET merchant_id = (
    SELECT id FROM (
        SELECT id FROM sys_user WHERE username = 'merchant' AND role = 'MERCHANT' LIMIT 1
    ) AS t
)
WHERE merchant_id IS NULL;

-- ------------------------------------------------------------
-- 4. 校验：应全部有归属（若为 0 则回填成功）
-- ------------------------------------------------------------
-- SELECT COUNT(*) AS 未归属商品数 FROM product WHERE merchant_id IS NULL;
