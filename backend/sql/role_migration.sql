-- 角色体系迁移脚本：添加 FARMER 角色和 farmer_id 字段
-- 在已运行的数据库上执行此脚本，然后重启后端（PasswordMigrationRunner 会自动加密明文密码）

USE agri_traceability;

-- 1. 添加 farmer_id 字段
ALTER TABLE sys_user ADD COLUMN farmer_id BIGINT DEFAULT NULL COMMENT '关联农户ID(FARMER角色使用)' AFTER role;

-- 2. 更新角色注释
ALTER TABLE sys_user MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CONSUMER' COMMENT '角色: ADMIN-系统管理员 MERCHANT-商家 FARMER-农户 CONSUMER-客户';

-- 3. 插入测试账号（明文密码，后端启动时 PasswordMigrationRunner 会自动加密）
INSERT INTO sys_user (username, password, real_name, role, farmer_id, phone, status) VALUES
('merchant', '123456', '测试商家', 'MERCHANT', NULL, '13900001001', 1),
('farmer01', '123456', '王建国', 'FARMER', 1, '13800001001', 1),
('consumer', '123456', '测试客户', 'CONSUMER', NULL, '13900002001', 1);
