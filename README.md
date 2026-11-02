# 茶药通 · 茶/中药材收购开单记账系统

> 面向茶叶、中药材产地收购场景的一体化记账系统：小程序开单 + PC 管理后台 + Spring Boot 后端，围绕「收购 → 欠款 → 日结」设计，收购员现场开单、老板每天算清账。

## 界面预览

**小程序端（产地开单）**

| 登录 | 开单 | 流水 |
|---|---|---|
| ![登录](screenshots/miniapp/02-登录.png) | ![开单](screenshots/miniapp/04-开单.png) | ![流水](screenshots/miniapp/05-流水.png) |

**PC 管理后台**

| 工作台 | 欠款管理 | 日结报表 |
|---|---|---|
| ![工作台](screenshots/pc/02-工作台.png) | ![欠款管理](screenshots/pc/06-欠款管理.png) | ![日结报表](screenshots/pc/07-日结报表.png) |

## 解决什么问题

茶/中药材收购的特点：**单子密、毛重皮重折算繁、赊账常态化**。

- 收购员在茶山/药市现场开单 → 小程序按「品名 → 毛重/皮重自动算净重金额」三步开单，支持磅单拍照留档
- 赊账是常态 → 每笔单据自带付款状态，欠款按客户维度聚合，谁欠多少一目了然
- 老板每天对账 → 日结报表自动汇总当日收购额、单数、欠款，支持 Excel 导出

## 技术架构

```
miniapp（uni-app + Vue3 + TS）    收购员现场开单
admin（Vue3 + Element Plus）      老板/会计对账
        │                            │
        └──────────┬─────────────────┘
                   ▼
server（Spring Boot 3 + MyBatis-Plus + JWT）
                   │
                   ▼
              MySQL（sql/chayetong.sql 一键建库）
```

| 模块 | 技术栈 | 说明 |
|---|---|---|
| `miniapp` | uni-app、Vue 3、TypeScript、Vite | 微信小程序端：开单 / 流水 / 欠款 / 我的 |
| `admin` | Vue 3、Element Plus、Pinia、ECharts | PC 管理后台：工作台 / 收购开单 / 结算对账 / 欠款 / 日结 / 档案 / 用户 |
| `server` | Spring Boot、MyBatis-Plus、JWT、MySQL | 单体后端，`CYT_*` 环境变量覆盖配置，本地与云上一套代码 |
| `deploy` | Docker Compose、Nginx | 云端一键部署：MySQL + 后端 + 前端静态托管 |
| `sql` | MySQL | 建库脚本 + 演示数据 |

## 快速开始

```bash
# 1. 数据库
mysql -uroot -p < sql/chayetong.sql

# 2. 后端（默认连 localhost:3306/chayetong，账号 root/123456，可用 CYT_* 环境变量覆盖）
cd server && mvn spring-boot:run

# 3. PC 管理后台
cd admin && npm install && npm run dev

# 4. 小程序（HBuilderX 或 CLI）
cd miniapp && npm install && npm run dev:mp-weixin
```

## 说明

- 仓库内数据均为**演示数据**（品名、客户、手机号皆为虚构），不含任何真实客户信息
- 项目由本人独立完成（需求、设计、前后端开发、部署）
