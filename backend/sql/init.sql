-- 农产品溯源销售系统 数据库初始化脚本
-- 数据库: agri_traceability
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS agri_traceability DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE agri_traceability;

-- 1. 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'CONSUMER' COMMENT '角色: ADMIN-系统管理员 MERCHANT-商家 FARMER-农户 CONSUMER-客户',
    farmer_id   BIGINT       DEFAULT NULL COMMENT '关联农户ID(FARMER角色使用)',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status      INT          DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 商品分类表
DROP TABLE IF EXISTS product_category;
CREATE TABLE product_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    sort        INT          DEFAULT 0 COMMENT '排序号',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 3. 商品表
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    category_id BIGINT        DEFAULT NULL COMMENT '分类ID',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock       INT           DEFAULT 0 COMMENT '库存',
    description TEXT          DEFAULT NULL COMMENT '商品描述',
    image       VARCHAR(255)  DEFAULT NULL COMMENT '商品图片URL',
    origin      VARCHAR(100)  DEFAULT NULL COMMENT '产地',
    status      INT           DEFAULT 1 COMMENT '状态: 1-上架 0-下架',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 4. 购物车表
DROP TABLE IF EXISTS cart;
CREATE TABLE cart (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    quantity    INT      DEFAULT 1 COMMENT '数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 5. 订单表
DROP TABLE IF EXISTS order_info;
CREATE TABLE order_info (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no         VARCHAR(50)   NOT NULL COMMENT '订单编号',
    user_id          BIGINT        NOT NULL COMMENT '用户ID',
    total_amount     DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status           INT          DEFAULT 0 COMMENT '状态: 0-待付款 1-已付款 2-已发货 3-已完成 4-已取消',
    receiver_name    VARCHAR(50)   DEFAULT NULL COMMENT '收货人',
    receiver_phone   VARCHAR(20)   DEFAULT NULL COMMENT '收货电话',
    receiver_address VARCHAR(255)  DEFAULT NULL COMMENT '收货地址',
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time         DATETIME      DEFAULT NULL COMMENT '付款时间',
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 6. 订单明细表
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id     BIGINT        NOT NULL COMMENT '订单ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100)  DEFAULT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) DEFAULT NULL COMMENT '商品单价',
    quantity     INT           DEFAULT NULL COMMENT '购买数量',
    subtotal     DECIMAL(10,2) DEFAULT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 7. 溯源批次表
DROP TABLE IF EXISTS trace_batch;
CREATE TABLE trace_batch (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次ID',
    batch_no    VARCHAR(50)  NOT NULL COMMENT '批次编号',
    product_id  BIGINT       NOT NULL COMMENT '关联商品ID',
    trace_code  VARCHAR(100) NOT NULL COMMENT '溯源码',
    status      INT          DEFAULT 0 COMMENT '状态: 0-生产中 1-质检中 2-运输中 3-已到货',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    UNIQUE KEY uk_trace_code (trace_code),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源批次表';

-- 8. 产地信息表
DROP TABLE IF EXISTS origin_info;
CREATE TABLE origin_info (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '产地信息ID',
    batch_id    BIGINT       NOT NULL COMMENT '关联批次ID',
    farm_name   VARCHAR(100) DEFAULT NULL COMMENT '农场名称',
    location    VARCHAR(255) DEFAULT NULL COMMENT '产地位置',
    farmer_name VARCHAR(50)  DEFAULT NULL COMMENT '农户名称',
    plant_date  DATE         DEFAULT NULL COMMENT '种植日期',
    harvest_date DATE        DEFAULT NULL COMMENT '采收日期',
    description TEXT         DEFAULT NULL COMMENT '产地描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产地信息表';

-- 9. 质检信息表
DROP TABLE IF EXISTS quality_inspection;
CREATE TABLE quality_inspection (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '质检信息ID',
    batch_id         BIGINT       NOT NULL COMMENT '关联批次ID',
    inspector        VARCHAR(50)  DEFAULT NULL COMMENT '质检员',
    inspection_date  DATETIME     DEFAULT NULL COMMENT '质检日期',
    inspection_item  VARCHAR(100) DEFAULT NULL COMMENT '检测项目',
    result           VARCHAR(255) DEFAULT NULL COMMENT '检测结果',
    qualified        INT          DEFAULT 1 COMMENT '是否合格: 1-合格 0-不合格',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检信息表';

-- 10. 物流信息表
DROP TABLE IF EXISTS logistics_info;
CREATE TABLE logistics_info (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '物流信息ID',
    batch_id         BIGINT       NOT NULL COMMENT '关联批次ID',
    ship_date        DATETIME     DEFAULT NULL COMMENT '发货日期',
    from_location    VARCHAR(255) DEFAULT NULL COMMENT '发货地',
    to_location      VARCHAR(255) DEFAULT NULL COMMENT '收货地',
    transport_company VARCHAR(100) DEFAULT NULL COMMENT '运输公司',
    transport_method VARCHAR(50)  DEFAULT NULL COMMENT '运输方式',
    temperature      VARCHAR(20)  DEFAULT NULL COMMENT '冷链温度',
    arrival_date     DATETIME     DEFAULT NULL COMMENT '到达日期',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息表';

-- 初始化数据：默认账号
-- admin 密码: admin123，其余密码: 123456
INSERT INTO sys_user (username, password, real_name, role, farmer_id, phone, status) VALUES
('admin', 'admin123', '系统管理员', 'ADMIN', NULL, '13800000001', 1),
('merchant', '123456', '测试商家', 'MERCHANT', NULL, '13900001001', 1),
('farmer01', '123456', '王建国', 'FARMER', 1, '13800001001', 1),
('consumer', '123456', '测试客户', 'CONSUMER', NULL, '13900002001', 1);

-- 初始化数据：商品分类
INSERT INTO product_category (name, description, sort) VALUES
('水果', '新鲜水果类农产品', 1),
('蔬菜', '新鲜蔬菜类农产品', 2),
('粮油', '粮油作物类产品', 3),
('畜禽', '畜禽肉类产品', 4);

-- 初始化数据：示例商品
INSERT INTO product (name, category_id, price, stock, description, origin, status) VALUES
('海南金枕榴莲', 1, 89.90, 100, '海南三亚直发，金枕榴莲，果肉饱满，香甜软糯', '海南三亚', 1),
('赣南脐橙', 1, 39.90, 200, '赣南特产脐橙，皮薄汁多，甜度高', '江西赣州', 1),
('五常大米', 3, 59.90, 300, '黑龙江五常产区，当季新米，口感软糯', '黑龙江五常', 1),
('山东大葱', 2, 12.90, 150, '章丘大葱，葱白长，味道鲜', '山东章丘', 1);
