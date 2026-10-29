-- ============================================================
-- 本地图片资源绑定脚本（2026-10-29）
--
-- 背景（一定要看，不然会被误导）：
--   项目早期所有图片字段存的都是
--     https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=...&image_size=square
--   这是 Trae IDE 内部的「文生图」接口，不是公开图床。
--   2026-10-29 实测：该接口对任何 prompt 都 302 跳到
--     https://lf-cdn.trae.com.cn/obj/trae-ai-image/page_image/default.jpeg
--   也就是永远返回同一张通用占位图 —— 接口已经废了。
--
--   而项目里本来就有 41 张本地图片（frontend/public/images/，共 23MB），
--   由 Vite 以站点根路径 /images/xxx.jpg 直接对外提供（实测 HTTP 200）。
--   所以正确做法不是去修那个接口，而是把字段指向本地文件。
--
-- 改动范围：四张表共 34 个字段
--   product.image            15 条（15 个商品）
--   knowledge_article.cover_image 8 条
--   recipe.cover_image        4 条
--   farmer_info.region_image   7 条

-- 幂等：全部是 UPDATE，可反复执行。
-- 顺序：本脚本必须在 init.sql ~ expansion.sql 之后执行（依赖这些表里已有数据）。
-- ============================================================

USE agri_traceability;

-- ---------- 1. 商品主图（15） ----------
-- 说明：商品 4（山东大葱）与商品 8（山东章丘大葱）共用同一张葱图，
--       本地素材里只有一张 p-scallion.jpg，够用且不影响展示。
UPDATE product SET image = '/images/p-durian.jpg'      WHERE id = 1;   -- 海南金枕榴莲
UPDATE product SET image = '/images/p-orange.jpg'      WHERE id = 2;   -- 赣南脐橙
UPDATE product SET image = '/images/p-rice.jpg'        WHERE id = 3;   -- 五常大米
UPDATE product SET image = '/images/p-scallion.jpg'    WHERE id = 4;   -- 山东大葱
UPDATE product SET image = '/images/p-pomegranate.jpg' WHERE id = 5;   -- 云南蒙自石榴
UPDATE product SET image = '/images/p-apple.jpg'       WHERE id = 6;   -- 新疆阿克苏苹果
UPDATE product SET image = '/images/p-pomelo.jpg'      WHERE id = 7;   -- 福建平和琯溪蜜柚
UPDATE product SET image = '/images/p-scallion.jpg'    WHERE id = 8;   -- 山东章丘大葱
UPDATE product SET image = '/images/p-mushroom.jpg'    WHERE id = 9;   -- 云南松茸
UPDATE product SET image = '/images/p-lily.jpg'        WHERE id = 10;  -- 甘肃兰州百合
UPDATE product SET image = '/images/p-rice-bag.jpg'    WHERE id = 11;  -- 黑龙江五常稻花香大米5kg
UPDATE product SET image = '/images/p-millet.jpg'      WHERE id = 12;  -- 山西小米
UPDATE product SET image = '/images/p-blackbean.jpg'   WHERE id = 13;  -- 东北黑豆500g
UPDATE product SET image = '/images/p-lamb.jpg'        WHERE id = 14;  -- 宁夏盐池滩羊腿肉1kg
UPDATE product SET image = '/images/p-crayfish.jpg'    WHERE id = 15;  -- 湖北潜江小龙虾1kg

-- ---------- 2. 农业百科封面（8） ----------
UPDATE knowledge_article SET cover_image = '/images/k-durian-guide.jpg' WHERE id = 1;  -- 如何挑选一颗好榴莲
UPDATE knowledge_article SET cover_image = '/images/k-orange.jpg'       WHERE id = 2;  -- 赣南脐橙的营养价值
UPDATE knowledge_article SET cover_image = '/images/k-ricestorage.jpg'  WHERE id = 3;  -- 五常大米如何储存保鲜
UPDATE knowledge_article SET cover_image = '/images/k-durian.jpg'       WHERE id = 4;  -- 榴莲的食用禁忌
UPDATE knowledge_article SET cover_image = '/images/k-geo.jpg'          WHERE id = 5;  -- 什么是地理标志产品
UPDATE knowledge_article SET cover_image = '/images/k-scallion.jpg'     WHERE id = 6;  -- 大葱的营养价值
UPDATE knowledge_article SET cover_image = '/images/k-mushroom.jpg'     WHERE id = 7;  -- 松茸为什么被称为菌中之王
UPDATE knowledge_article SET cover_image = '/images/k-storage.jpg'      WHERE id = 8;  -- 冬季农产品储存全攻略

-- ---------- 3. 农产食谱封面（4） ----------
UPDATE recipe SET cover_image = '/images/r-cake.jpg'      WHERE id = 1;  -- 榴莲千层蛋糕
UPDATE recipe SET cover_image = '/images/r-salad.jpg'     WHERE id = 2;  -- 脐橙鲜虾沙拉
UPDATE recipe SET cover_image = '/images/r-friedrice.jpg' WHERE id = 3;  -- 五常米蛋炒饭
UPDATE recipe SET cover_image = '/images/r-stirfry.jpg'   WHERE id = 4;  -- 章丘大葱爆羊肉

-- ---------- 4. 农户产地实景（7） ----------
-- 按 location 里的省份配对，不用 id 硬编码省份含义，换数据也不会错位
UPDATE farmer_info SET region_image = '/images/f-hainan.jpg'       WHERE location LIKE '海南省%';        -- 王建国 三亚
UPDATE farmer_info SET region_image = '/images/f-jiangxi.jpg'      WHERE location LIKE '江西省%';        -- 李明华 赣州
UPDATE farmer_info SET region_image = '/images/f-heilongjiang.jpg' WHERE location LIKE '黑龙江省%';      -- 张富贵 五常
UPDATE farmer_info SET region_image = '/images/f-shandong.jpg'     WHERE location LIKE '山东省%';        -- 刘大伟 章丘
UPDATE farmer_info SET region_image = '/images/f-shaanxi.jpg'      WHERE location LIKE '陕西省%';        -- 李秀兰 洛川
UPDATE farmer_info SET region_image = '/images/f-yunnan.jpg'       WHERE location LIKE '云南省%';        -- 扎西顿珠 香格里拉
UPDATE farmer_info SET region_image = '/images/f-gansu.jpg'        WHERE location LIKE '甘肃省%';        -- 陈小芳 兰州

-- ---------- 5. 自检：残留的外链图片地址必须为 0 ----------
-- 正常应输出 4 行、残留数全为 0；若有非 0，说明还有字段没覆盖到。
SELECT '残留外链-商品' AS 检查项, COUNT(*) AS 条数 FROM product
  WHERE image IS NOT NULL AND image NOT LIKE '/images/%'
UNION ALL
SELECT '残留外链-百科', COUNT(*) FROM knowledge_article
  WHERE cover_image IS NOT NULL AND cover_image NOT LIKE '/images/%'
UNION ALL
SELECT '残留外链-食谱', COUNT(*) FROM recipe
  WHERE cover_image IS NOT NULL AND cover_image NOT LIKE '/images/%'
UNION ALL
SELECT '残留外链-农户', COUNT(*) FROM farmer_info
  WHERE region_image IS NOT NULL AND region_image NOT LIKE '/images/%';
