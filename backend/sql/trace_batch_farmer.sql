-- ============================================================
-- 溯源批次-农户归属迁移脚本
-- 文件：backend/sql/trace_batch_farmer.sql
-- 创建：2026-10-19
-- ------------------------------------------------------------
-- 目的：
--   trace_batch 原本只有 product_id，没有任何"这批货是谁种的"信息。
--   后台农户账号（role=FARMER）登录后，溯源管理页看到的是全平台批次，
--   属于横向越权，也无法支撑"农户只看自家批次"的业务口径。
--   本脚本给批次补上"供货农户"，作为农户侧越权的判定依据。
--
-- 约定：
--   farmer_id = NULL  表示"未指定农户"（平台/商家代管，只有 ADMIN 可管理）
--   farmer_id = 某个 farmer_info.id，表示该批次由该农户生产供货
--
--   为什么不通过 product_id 反推农户：
--   同一商品可能由多个农户供货，商品级的单一农户字段表达不了；
--   批次才是"一次具体的生产发货"，归到批次上位语义是对的。
--
-- 执行方式（Windows，注意路径带空格）：
--   "D:/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p agri_traceability \
--       --default-character-set=utf8mb4 < trace_batch_farmer.sql
--
-- 【坑，务必保留 SET NAMES】
--   Windows 下 mysql 客户端从文件重定向读取时默认按 GBK 解析，
--   本脚本里的中文匹配条件（origin LIKE '%海南%'、farmer_info.name='王建国'）
--   会全部匹配不上，表现为"脚本执行无报错，但回填结果全是 NULL"。
--   下面的 SET NAMES utf8mb4 是流式生效的，能修正后续语句的解析，
--   执行命令里的 --default-character-set 是双保险。
--
-- 回滚方式：
--   1) 先还原数据（备份表里有全量旧数据）：
--        DELETE FROM trace_batch;
--        INSERT INTO trace_batch (id, batch_no, product_id, trace_code, status, create_time, update_time)
--        SELECT id, batch_no, product_id, trace_code, status, create_time, update_time
--          FROM trace_batch_bak_20261019;
--   2) 再删列：
--        ALTER TABLE trace_batch DROP COLUMN farmer_id;
-- ============================================================

USE agri_traceability;

-- 必须放在所有中文匹配语句之前：修正客户端按 GBK 解析导致的匹配失败
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 0. 执行前备份
--    已在执行前手工跑过：CREATE TABLE trace_batch_bak_20261019 AS SELECT * FROM trace_batch;
--    这里保留语句作为文档，重复执行前请自行确认备份表是否已存在。
-- ------------------------------------------------------------
-- CREATE TABLE trace_batch_bak_20261019 AS SELECT * FROM trace_batch;

-- ------------------------------------------------------------
-- 1. 新增归属字段
--    MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--    若已加过会报 1060 Duplicate column name，忽略即可
-- ------------------------------------------------------------
ALTER TABLE trace_batch
    ADD COLUMN farmer_id BIGINT DEFAULT NULL
    COMMENT '供货农户ID(关联farmer_info.id)，NULL=未指定农户'
    AFTER product_id;

-- ------------------------------------------------------------
-- 2. 新增索引
--    农户后台按 farmer_id 过滤批次，必须走索引
-- ------------------------------------------------------------
ALTER TABLE trace_batch ADD KEY idx_farmer (farmer_id);

-- ------------------------------------------------------------
-- 3. 历史数据回填（按产地精确匹配，不做模糊猜测）
--    匹配原则：商品产地与农户所在地必须指向同一产区，否则留 NULL。
--    这些批次原本没有农户信息，回填只是让演示数据可用，
--    真实项目里应当由管理员在溯源管理页逐条指派。
-- ------------------------------------------------------------
SET @f_hainan      = (SELECT id FROM farmer_info WHERE name = '王建国'   LIMIT 1);
SET @f_ganzhou     = (SELECT id FROM farmer_info WHERE name = '李明华'   LIMIT 1);
SET @f_wuchang     = (SELECT id FROM farmer_info WHERE name = '张富贵'   LIMIT 1);
SET @f_zhangqiu    = (SELECT id FROM farmer_info WHERE name = '刘大伟'   LIMIT 1);
SET @f_shangrila   = (SELECT id FROM farmer_info WHERE name = '扎西顿珠' LIMIT 1);
SET @f_lanzhou     = (SELECT id FROM farmer_info WHERE name = '陈小芳'   LIMIT 1);

-- 海南三亚（榴莲）→ 王建国
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_hainan
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%海南%';

-- 江西赣州（脐橙）→ 李明华
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_ganzhou
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%赣州%';

-- 黑龙江五常（大米）→ 张富贵
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_wuchang
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%五常%';

-- 山东章丘（大葱）→ 刘大伟
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_zhangqiu
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%章丘%';

-- 云南香格里拉（松茸）→ 扎西顿珠
-- 注意：云南蒙自石榴不属于香格里拉产区，不会被这句匹配，保持 NULL
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_shangrila
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%香格里拉%';

-- 甘肃兰州（百合）→ 陈小芳
UPDATE trace_batch tb JOIN product p ON p.id = tb.product_id
   SET tb.farmer_id = @f_lanzhou
 WHERE tb.farmer_id IS NULL AND p.origin LIKE '%兰州%';

-- ------------------------------------------------------------
-- 4. 校验
--    expected：约 10 条有农户，其余为 NULL（蒙自石榴/阿克苏苹果/平和蜜柚/
--    山西小米/东北黑豆/盐池滩羊/潜江小龙虾 等没有对应农户记录）
-- ------------------------------------------------------------
-- SELECT IFNULL(farmer_id, -1) AS 农户, COUNT(*) AS 批次
--   FROM trace_batch GROUP BY farmer_id ORDER BY farmer_id;
