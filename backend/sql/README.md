# 数据库脚本说明

数据库名：`agri_traceability`，字符集 `utf8mb4`。

本目录共 8 个 SQL 脚本，累计创建 **23 张表**。脚本之间**存在依赖关系**，请按下方顺序执行。

---

## 一、脚本清单

| 序号 | 脚本 | 作用 | 新建的表 | 是否含初始数据 |
|:---:|---|---|---|:---:|
| 1 | `init.sql` | 建库 + 核心业务表 + 基础数据 | `sys_user`、`product_category`、`product`、`cart`、`order_info`、`order_item`、`trace_batch`、`origin_info`、`quality_inspection`、`logistics_info`（共 10 张） | 是（用户 / 分类 / 商品） |
| 2 | `new_modules.sql` | 农户、评价、公告模块 | `farmer_info`、`product_review`、`announcement` | 是 |
| 3 | `missing_tables.sql` | 补齐通知、地址、收藏三张表 | `notification`、`user_address`、`user_favorite` | 否 |
| 4 | `expansion.sql` | 功能扩展：时令、百科、食谱、问答；并为 `farmer_info` 增加地图字段 | `product_seasonal`、`knowledge_article`、`recipe`、`recipe_product`、`faq`；并 `ALTER TABLE farmer_info` 增加 5 列（经度 / 纬度 / 区域标签 / 产地描述 / 产地图片） | 是（商品、农户、公告、食谱等） |
| 5 | `quality_standards.sql` | 把原先硬编码在 Controller 里的质量标准迁入数据库 | `quality_standards`、`trace_process_nodes` | 是 |
| 6 | `trace_sample_data.sql` | 4 条完整溯源链示例数据（批次 + 产地 + 质检 + 物流） | 无（仅 INSERT） | 是 |
| 7 | `update_images.sql` | 为商品（id 1~15）回填图片 URL | 无（仅 UPDATE） | — |
| 8 | `role_migration.sql` | **历史迁移脚本**，用于给旧库补 `FARMER` 角色 | 无（仅 ALTER + INSERT） | 是（测试账号） |

---

## 二、推荐执行顺序（全新环境）

```bash
mysql -u root -p < init.sql               # 1. 建库 + 10 张核心表 + 基础数据
mysql -u root -p < new_modules.sql        # 2. 农户 / 评价 / 公告
mysql -u root -p < missing_tables.sql     # 3. 通知 / 地址 / 收藏
mysql -u root -p < expansion.sql          # 4. 扩展模块（必须在 new_modules.sql 之后）
mysql -u root -p < quality_standards.sql  # 5. 质量标准
mysql -u root -p < trace_sample_data.sql  # 6. 溯源示例数据
mysql -u root -p < update_images.sql      # 7. 商品图片
```

> **第 8 个脚本 `role_migration.sql` 在全新环境下不要执行**，原因见下文「三、注意事项」。

---

## 三、注意事项（执行前必读）

### 1. `init.sql` 会清空已有数据

`init.sql` 中 10 张表全部使用 `DROP TABLE IF EXISTS` 后再 `CREATE TABLE`。
**在已有数据的库上执行会导致业务数据全部丢失**，仅用于首次初始化或可丢弃的本地环境。

### 2. `expansion.sql` 必须在 `new_modules.sql` 之后执行

`expansion.sql` 第 256~274 行是 4 条按姓名回填经纬度的语句：

```sql
UPDATE farmer_info SET longitude=109.5083, latitude=18.3200, ... WHERE name='王建国';
-- 另有 李明华 / 张富贵 / 刘大伟 三条，共 4 条
```

这些 UPDATE 依赖 `new_modules.sql` 已经插入的 4 条农户数据。顺序颠倒时语句会匹配 0 行（不报错，但地图字段为空、产地地图无坐标）。

### 3. `role_migration.sql` 与新版 `init.sql` 冲突

`init.sql` 的 `sys_user` 表**已经包含** `farmer_id` 字段和四角色注释，且已插入 `merchant` / `farmer01` / `consumer` 三个账号。
若在 `init.sql` 之后再执行 `role_migration.sql`，会直接报两类错误：

| 语句 | 报错 |
|---|---|
| `ALTER TABLE sys_user ADD COLUMN farmer_id ...` | `ERROR 1060 - Duplicate column name 'farmer_id'` |
| `INSERT INTO sys_user ... ('merchant', ...)` | `ERROR 1062 - Duplicate entry 'merchant' for key 'uk_username'` |

**该脚本仅用于升级早期版本数据库（当时 `sys_user` 还没有 `farmer_id`）。全新安装请跳过。**

### 4. `announcement.type` 存在类型不一致（已知问题）

- 表定义（`new_modules.sql:36`）：`type INT DEFAULT 0 COMMENT '类型 0系统公告 1活动 2通知'`
- `new_modules.sql` 插入的是数字：`0 / 1 / 2`
- `expansion.sql:297` 插入的是字符串：`'ACTIVITY' / 'NOTICE' / 'SYSTEM'`

在 MySQL 8 默认严格模式（`STRICT_TRANS_TABLES`）下，字符串写入 `INT` 列会触发 `ERROR 1366 - Incorrect integer value`；非严格模式下会被静默转成 `0` 并产生 warning，导致公告分类失效。

后端实体 `Announcement.java` 中该字段为 `private Integer type`，与表定义一致。
**若要修复，应把 `expansion.sql` 中的字符串改为 `0/1/2`，或把列改为 `VARCHAR`。**（本次未改动任何 SQL 文件内容。）

### 5. `update_images.sql` 带 BOM 头

文件首字节为 UTF-8 BOM（`EF BB BF`）。多数客户端（MySQL CLI、Navicat、IDEA）可正常处理，个别工具可能把首个 `USE` 语句识别失败。如遇问题，去掉 BOM 后重新执行即可，内容不受影响。

### 6. 初始密码是明文，启动后自动加密

`init.sql` 中密码以明文存放。后端启动时 `PasswordMigrationRunner` 会扫描 `sys_user`，把不以 `$2a$` 开头的密码用 BCrypt 重新加密并回写；登录校验使用 `passwordEncoder.matches()`，因此**首次启动后用明文密码仍可正常登录**，之后再改密码就以密文为准。

> 明文初始密码**不写进本文档**（仓库是公开的）。需要时直接查 `init.sql`。

---

## 四、初始化后的默认账号

| 用户名 | 角色 | 说明 |
|---|---|---|
| `admin` | `ADMIN` | 系统管理员，全部后台权限 |
| `merchant` | `MERCHANT` | 商家，自己名下商品 / 含自家商品的订单 / 溯源批次 / 库存预警 / 待发货工作台 |
| `farmer01` | `FARMER` | 农户（关联 `farmer_info.id = 1` 王建国），自家产地与溯源批次 |
| `consumer` | `CONSUMER` | 消费者，下单 / 收藏 / 溯源查询 |

> 初始密码只在 `init.sql` 里，本文档不复制。**首次登录后请立即修改；生产环境请删除全部演示账号。**
> 角色权限映射见前端 `frontend/src/views/admin/AdminLayout.vue` 中的 `rolePermissions`。
