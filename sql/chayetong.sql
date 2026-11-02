-- ============================================================
-- 茶药通 · 产地收购开单记账  建库建表 + 演示种子数据
-- 可重复执行：内部先 DROP 再 CREATE
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/chayetong.sql
-- ============================================================

DROP DATABASE IF EXISTS chayetong;
CREATE DATABASE chayetong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE chayetong;

-- ---------------- 账号（老板 ADMIN / 开单员 STAFF） ----------------
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  role        VARCHAR(10)  NOT NULL DEFAULT 'STAFF' COMMENT 'ADMIN 老板 / STAFF 开单员',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 停用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='账号';

-- ---------------- 果品档案（品类 + 规格 + 预设单价） ----------------
CREATE TABLE fruit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL COMMENT '品类，如 沃柑',
  spec        VARCHAR(50)  NOT NULL DEFAULT '统货' COMMENT '规格等级，如 大果/中果',
  price       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '默认收购单价（元）',
  unit        VARCHAR(10)  NOT NULL DEFAULT '斤' COMMENT '计量单位',
  enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_name_spec (name, spec)
) ENGINE=InnoDB COMMENT='果品档案';

-- ---------------- 收购单（核心表） ----------------
CREATE TABLE purchase_order (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  order_no       VARCHAR(32)   NOT NULL COMMENT '单号 CG+yyyyMMdd+序号',
  fruit_id       BIGINT        NOT NULL COMMENT '果品档案 id',
  fruit_name     VARCHAR(50)   NOT NULL COMMENT '品类（冗余快照）',
  spec           VARCHAR(50)   NOT NULL COMMENT '规格（冗余快照）',
  gross_weight   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '毛重',
  tare_weight    DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '皮重',
  net_weight     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '净重 = 毛重 - 皮重',
  price          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '成交单价（元）',
  amount         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额 = 净重 × 单价',
  farmer_name    VARCHAR(50)   DEFAULT NULL COMMENT '果农姓名',
  farmer_phone   VARCHAR(20)   DEFAULT NULL COMMENT '果农电话',
  source         VARCHAR(20)   DEFAULT NULL COMMENT '来源：田头/市场/合作社',
  pay_status     VARCHAR(10)   NOT NULL DEFAULT 'UNPAID' COMMENT 'PAID 已付 / UNPAID 未付',
  pay_method     VARCHAR(20)   DEFAULT NULL COMMENT '现金/微信/转账',
  photo_url      VARCHAR(255)  DEFAULT NULL COMMENT '磅单照片',
  remark         VARCHAR(255)  DEFAULT NULL,
  created_by     BIGINT        DEFAULT NULL COMMENT '开单人 id',
  created_by_name VARCHAR(50)  DEFAULT NULL COMMENT '开单人姓名',
  order_date     DATE          NOT NULL COMMENT '收购日期（日结维度）',
  created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_date (order_date),
  KEY idx_farmer (farmer_name),
  KEY idx_pay_status (pay_status)
) ENGINE=InnoDB COMMENT='收购单';

-- ============================================================
-- 演示种子数据
-- 账号密码均为 123456（BCrypt）
-- ============================================================
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('admin',   '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '黄老板', '13800000001', 'ADMIN', 1),
('kaipan',  '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '阿强',   '13800000002', 'STAFF', 1);

INSERT INTO fruit (name, spec, price, unit, enabled) VALUES
('沃柑',    '大果', 3.20, '斤', 1),
('沃柑',    '中果', 2.60, '斤', 1),
('砂糖橘',  '统货', 2.80, '斤', 1),
('砂糖橘',  '中果', 3.10, '斤', 1),
('百香果',  '一级', 4.50, '斤', 1),
('荔枝',    '妃子笑', 5.80, '斤', 1);

-- 演示收购单：覆盖最近 7 天（供看板趋势/日结演示），含未付欠款
INSERT INTO purchase_order
  (order_no, fruit_id, fruit_name, spec, gross_weight, tare_weight, net_weight, price, amount,
   farmer_name, farmer_phone, source, pay_status, pay_method, remark, created_by, created_by_name, order_date)
VALUES
  ('CG20261015001', 1, '沃柑',   '大果',   1050, 50, 1000, 3.20, 3200.00, '老黄',   '13800000001', '田头', 'PAID',   '微信', NULL, 1, '黄老板', '2026-10-15'),
  ('CG20261015002', 3, '砂糖橘', '统货',    820, 20,  800, 2.80, 2240.00, '韦秀兰', '13800000002', '市场', 'UNPAID', NULL, '先欠着', 1, '黄老板', '2026-10-15'),
  ('CG20261015003', 5, '百香果', '一级',    310, 10,  300, 4.50, 1350.00, '阿明',   '13800000003', '田头', 'PAID',   '现金', NULL, 2, '阿强',   '2026-10-15'),
  ('CG20261016001', 1, '沃柑',   '中果',   1260, 60, 1200, 2.60, 3120.00, '老黄',   '13800000001', '田头', 'PAID',   '微信', NULL, 1, '黄老板', '2026-10-16'),
  ('CG20261016002', 2, '沃柑',   '中果',    950, 50,  900, 2.60, 2340.00, '蓝姐',   '13800000004', '合作社', 'UNPAID', NULL, '月底结', 2, '阿强',   '2026-10-16'),
  ('CG20261017001', 3, '砂糖橘', '统货',   1540, 40, 1500, 2.80, 4200.00, '韦秀兰', '13800000002', '市场', 'PAID',   '转账', NULL, 1, '黄老板', '2026-10-17'),
  ('CG20261017002', 6, '荔枝',   '妃子笑',  520, 20,  500, 5.80, 2900.00, '阿明',   '13800000003', '田头', 'PAID',   '微信', NULL, 2, '阿强',   '2026-10-17'),
  ('CG20261017003', 1, '沃柑',   '大果',   1100, 50, 1050, 3.20, 3360.00, '老黄',   '13800000001', '田头', 'UNPAID', NULL, '过两天来收钱', 1, '黄老板', '2026-10-17'),
  ('CG20261018001', 4, '砂糖橘', '中果',    930, 30,  900, 3.10, 2790.00, '蓝姐',   '13800000004', '合作社', 'PAID',   '现金', NULL, 2, '阿强',   '2026-10-18'),
  ('CG20261018002', 5, '百香果', '一级',    420, 20,  400, 4.50, 1800.00, '覃叔',   '13800000005', '市场', 'PAID',   '微信', NULL, 1, '黄老板', '2026-10-18'),
  ('CG20261019001', 1, '沃柑',   '大果',   1550, 50, 1500, 3.20, 4800.00, '老黄',   '13800000001', '田头', 'PAID',   '微信', NULL, 1, '黄老板', '2026-10-19'),
  ('CG20261019002', 3, '砂糖橘', '统货',   1120, 20, 1100, 2.80, 3080.00, '韦秀兰', '13800000002', '市场', 'UNPAID', NULL, '果农要现金', 2, '阿强',   '2026-10-19'),
  ('CG20261019003', 6, '荔枝',   '妃子笑',  320, 20,  300, 5.80, 1740.00, '覃叔',   '13800000005', '田头', 'PAID',   '现金', NULL, 2, '阿强',   '2026-10-19'),
  ('CG20261020001', 2, '沃柑',   '中果',   1050, 50, 1000, 2.60, 2600.00, '蓝姐',   '13800000004', '合作社', 'PAID',   '转账', NULL, 1, '黄老板', '2026-10-20'),
  ('CG20261020002', 4, '砂糖橘', '中果',   1240, 40, 1200, 3.10, 3720.00, '韦秀兰', '13800000002', '市场', 'PAID',   '微信', NULL, 2, '阿强',   '2026-10-20'),
  ('CG20261020003', 5, '百香果', '一级',    530, 30,  500, 4.50, 2250.00, '阿明',   '13800000003', '田头', 'UNPAID', NULL, NULL, 1, '黄老板', '2026-10-20'),
  ('CG20261021001', 1, '沃柑',   '大果',   1060, 60, 1000, 3.20, 3200.00, '老黄',   '13800000001', '田头', 'PAID',   '微信', NULL, 1, '黄老板', '2026-10-21'),
  ('CG20261021002', 3, '砂糖橘', '统货',    840, 40,  800, 2.80, 2240.00, '覃叔',   '13800000005', '市场', 'UNPAID', NULL, '明天来拿钱', 2, '阿强',   '2026-10-21'),
  ('CG20261021003', 2, '沃柑',   '中果',    770, 70,  700, 2.60, 1820.00, '蓝姐',   '13800000004', '合作社', 'PAID',   '现金', NULL, 2, '阿强',   '2026-10-21');
