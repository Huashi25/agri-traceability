-- ================================================================
-- 质量标准数据表 + 初始化数据
-- 将硬编码的质量标准从Controller迁移到数据库，实现数据持久化
-- ================================================================

-- 质量标准表
CREATE TABLE IF NOT EXISTS quality_standards (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sort_num    VARCHAR(10)  NOT NULL COMMENT '序号',
    title       VARCHAR(100) NOT NULL COMMENT '标准标题',
    code        VARCHAR(50)  NOT NULL COMMENT '标准编号',
    description TEXT         NOT NULL COMMENT '标准描述',
    icon        VARCHAR(100) NOT NULL COMMENT '图标CSS类名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量标准信息表';

-- 溯源流程节点表
CREATE TABLE IF NOT EXISTS trace_process_nodes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sort_num    VARCHAR(10)  NOT NULL COMMENT '序号',
    title       VARCHAR(100) NOT NULL COMMENT '流程标题',
    stage       VARCHAR(50)  NOT NULL COMMENT '阶段',
    description TEXT         NOT NULL COMMENT '流程描述',
    icon        VARCHAR(100) NOT NULL COMMENT '图标CSS类名',
    color       VARCHAR(20)  NOT NULL COMMENT '主题色',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源流程节点表';

-- 初始化质量标准数据
INSERT INTO quality_standards (sort_num, title, code, description, icon) VALUES
('01', '农药残留检测', 'GB 2763-2021', '所有农产品上市前必须通过农药残留检测，限量标准严格遵循国家标准。共检测有机磷、有机氯、拟除虫菊酯等6大类50余项指标。', 'fas fa-flask'),
('02', '重金属含量检测', 'GB 2762-2022', '检测铅、镉、汞、砷等重金属含量，确保土壤和水源安全，防止重金属通过食物链进入人体。', 'fas fa-atom'),
('03', '微生物检测', 'GB 29921-2021', '检测大肠菌群、沙门氏菌、金黄色葡萄球菌等致病菌，保障食品微生物安全。', 'fas fa-virus'),
('04', '产地环境认证', 'NY/T 5295-2015', '产地土壤、水源、空气质量检测认证，确保生产环境无污染。包括土壤重金属、水质pH、空气二氧化硫等指标。', 'fas fa-mountain'),
('05', '冷链温控标准', 'GB/T 28843-2012', '全程2-8°C冷链运输，实时温度监控记录。每批次农产品从采摘到配送全程温度可追溯。', 'fas fa-temperature-low'),
('06', '溯源信息完整', 'GB/T 38163-2019', '每批次产品必须包含完整溯源信息：产地、农户、采摘时间、质检报告、物流记录，做到来源可查、去向可追。', 'fas fa-clipboard-check')
ON DUPLICATE KEY UPDATE title=VALUES(title), description=VALUES(description);

-- 初始化溯源流程节点数据
INSERT INTO trace_process_nodes (sort_num, title, stage, description, icon, color) VALUES
('01', '种植记录', '种植', '产地环境认证，种植过程记录，农事操作日志', 'fas fa-seedling', '#2d8c3c'),
('02', '采摘入库', '采摘', '成熟度检测，分拣入库，批次编码', 'fas fa-tractor', '#3b7ddd'),
('03', '质量检测', '质检', '农残检测，重金属检测，微生物检测', 'fas fa-vial', '#9c27b0'),
('04', '冷链运输', '物流', '冷链温控，全程追踪，签收确认', 'fas fa-truck', '#ff9800');
