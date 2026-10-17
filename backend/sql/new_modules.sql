USE agri_traceability;

-- 农户信息表
CREATE TABLE IF NOT EXISTS farmer_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '农户姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    farm_name VARCHAR(100) COMMENT '农场名称',
    location VARCHAR(200) COMMENT '农场位置',
    farm_area DECIMAL(10,2) COMMENT '种植面积(亩)',
    main_crop VARCHAR(100) COMMENT '主要作物',
    description TEXT COMMENT '简介',
    status INT DEFAULT 1 COMMENT '状态 1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农户信息表';

-- 商品评价表
CREATE TABLE IF NOT EXISTS product_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户名',
    rating INT DEFAULT 5 COMMENT '评分1-5',
    content TEXT COMMENT '评价内容',
    images VARCHAR(500) COMMENT '评价图片(逗号分隔)',
    status INT DEFAULT 1 COMMENT '状态 1显示 0隐藏',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    type INT DEFAULT 0 COMMENT '类型 0系统公告 1活动 2通知',
    status INT DEFAULT 1 COMMENT '状态 1发布 0草稿',
    sort INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 插入示例数据
INSERT INTO farmer_info (name, phone, farm_name, location, farm_area, main_crop, description) VALUES
('王建国', '13800001001', '三亚金辉热带果园', '海南省三亚市崖州区南滨农场', 120.00, '榴莲、芒果', '20年热带水果种植经验，生态种植倡导者'),
('李明华', '13800001002', '赣州红土地脐橙合作社', '江西省赣州市信丰县安西镇', 350.00, '脐橙、柚子', '合作社带头人，带领50户农户共同致富'),
('张富贵', '13800001003', '五常稻花香种植基地', '黑龙江省哈尔滨市五常市民乐乡', 500.00, '稻花香大米', '黑土地第三代农民，鸭稻共生生态模式'),
('刘大伟', '13800001004', '章丘大葱示范田', '山东省济南市章丘区绣惠街道', 80.00, '章丘大葱', '地理标志保护产品传承人，葱白长达1米');

INSERT INTO product_review (product_id, user_id, user_name, rating, content) VALUES
(1, 2, 'testuser', 5, '榴莲非常新鲜，果肉饱满，溯源信息完整，吃得放心！'),
(1, 3, 'customer1', 4, '味道不错，就是到货稍微慢了点'),
(2, 2, 'testuser', 5, '脐橙很甜，包装好，产地直发的确实不一样');

INSERT INTO announcement (title, content, type, status, sort) VALUES
('欢迎使用农产品溯源销售系统', '本系统致力于为消费者提供从田间到餐桌的全程可追溯服务，保障食品安全。', 0, 1, 1),
('新一批海南金枕榴莲已上架', '三亚直采，冷链运输，溯源码可查全程信息。限时优惠，欢迎选购！', 1, 1, 2),
('系统维护通知', '本周日凌晨2:00-4:00系统将进行例行维护，届时无法访问，请提前安排。', 2, 1, 3);
