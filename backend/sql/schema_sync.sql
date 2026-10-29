-- ================================================================
-- 表结构同步脚本：补齐「实体已有、建表脚本却没有」的列
-- 建立日期：2026-10-29
-- 背景：
--   2026-10-28 删除 D 盘、重建数据库时发现 backend/sql 下的建表脚本
--   并不能从零建出可运行的库 —— 有 7 个实体字段在任何脚本里都没有对应的列，
--   推测是开发期间用 IDE 手工 ALTER 加上的，从未回写脚本。
--   缺列时 MyBatis-Plus 会把字段拼进 SELECT，直接报
--   Unknown column 'xxx' in 'field list'，对应接口全线 500。
--
-- 执行位置：在 init.sql / new_modules.sql / missing_tables.sql / expansion.sql
--           / quality_standards.sql / trace_sample_data.sql / update_images.sql
--           / product_merchant.sql / trace_batch_farmer.sql 之后执行。
--
-- 幂等说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--   重复执行会报 1060 Duplicate column name。重复执行前请先查
--   SHOW COLUMNS FROM product LIKE 'sales_count';
-- ================================================================

USE agri_traceability;

-- ----------------------------------------------------------------
-- 1. product：商品扩展字段
--    original_price 原价（划线价）  sales_count 销量
--    rating 平均评分（0.0~5.0）    review_count 评价数
-- 全部带默认值，历史数据不会因 NULL 导致前端展示异常
-- ----------------------------------------------------------------
ALTER TABLE product
    ADD COLUMN original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价（划线价）' AFTER price,
    ADD COLUMN sales_count    INT           DEFAULT 0    COMMENT '销量'            AFTER stock,
    ADD COLUMN rating         DECIMAL(2,1)  DEFAULT 0.0  COMMENT '平均评分'        AFTER sales_count,
    ADD COLUMN review_count   INT           DEFAULT 0    COMMENT '评价数量'        AFTER rating;

-- ----------------------------------------------------------------
-- 2. order_info：订单流程字段
--    payment_method 支付方式  ship_time 发货时间  receive_time 收货时间
-- 这三个字段在实体 OrderInfo 中已存在，控制器的发货/收货逻辑依赖它们
-- ----------------------------------------------------------------
ALTER TABLE order_info
    ADD COLUMN payment_method VARCHAR(20) DEFAULT NULL COMMENT '支付方式' AFTER receiver_address,
    ADD COLUMN ship_time      DATETIME    DEFAULT NULL COMMENT '发货时间' AFTER pay_time,
    ADD COLUMN receive_time   DATETIME    DEFAULT NULL COMMENT '收货时间' AFTER ship_time;

-- ----------------------------------------------------------------
-- 3. 校验：把上面补的列打印出来，执行完应当能看到 7 行
-- ----------------------------------------------------------------
SELECT 'product' AS tbl, COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'agri_traceability' AND TABLE_NAME = 'product'
  AND COLUMN_NAME IN ('original_price','sales_count','rating','review_count')
UNION ALL
SELECT 'order_info', COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'agri_traceability' AND TABLE_NAME = 'order_info'
  AND COLUMN_NAME IN ('payment_method','ship_time','receive_time');
