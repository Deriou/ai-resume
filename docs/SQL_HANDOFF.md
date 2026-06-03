# AiResume SQL 交接文档

本文档用于说明 AiResume 在本地测试和 P7 上云交接时如何初始化数据库表结构和演示数据。

## 1. SQL 文件

```text
sql/schema.sql
sql/data.sql
sql/bulk_demo_data.sql
```

- `schema.sql`: 创建 `db_airesume` 数据库和全部应用表。
- `data.sql`: 重置并写入一套可复现的演示数据。
- `data.sql` 用于演示环境初始化, 不用于保留生产真实数据。
- `bulk_demo_data.sql`: 可选批量演示数据, 用于触发分页、热门岗位、企业面板和管理员统计图表。

## 2. 导入顺序

必须先导入表结构, 再导入演示数据:

```bash
mysql --default-character-set=utf8mb4 -uroot -p < sql/schema.sql
mysql --default-character-set=utf8mb4 -uroot -p db_airesume < sql/data.sql
```

如果需要大量岗位、企业、用户和投递数据, 再额外导入:

```bash
mysql --default-character-set=utf8mb4 -uroot -p db_airesume < sql/bulk_demo_data.sql
```

注意:

```text
必须加 --default-character-set=utf8mb4
```

否则中文数据可能出现乱码。

## 3. 本地 Docker 导入方式

在项目根目录执行:

```bash
docker exec ai-resume-mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -proot123456 < /docker-entrypoint-initdb.d/schema.sql'
docker exec ai-resume-mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -proot123456 db_airesume < /docker-entrypoint-initdb.d/data.sql'
docker exec ai-resume-mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -proot123456 db_airesume < /docker-entrypoint-initdb.d/bulk_demo_data.sql'
```

## 4. 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 企业 | `enterprise` | `enterprise123` |
| 企业 | `enterprise_ops` | `enterprise123` |
| 求职者 | `user` | `user123` |
| 求职者 | `user_li` | `user123` |
| 求职者 | `user_chen` | `user123` |

## 5. 演示数据覆盖范围

当前种子数据包含:

- 1 个管理员账号。
- 2 个企业账号。
- 3 个求职者账号。
- 5 份简历。
- 7 个岗位, 包含开放和关闭状态。
- 8 条投递记录, 覆盖 `PENDING`, `VIEWED`, `ACCEPTED`, `REJECTED`。
- 简历评分记录。
- 简历润色记录。
- 岗位匹配记录。
- LLM 调用日志, 包含成功和失败记录。
- AI 币流水, 包含管理员发放、签到、AI 调用扣费。

说明:

```text
resume_file 表不预置数据。
```

原因是附件元数据如果没有对应的真实文件, 下载接口会失败。简历附件建议在前端演示时手动上传。

如果额外导入 `bulk_demo_data.sql`, 会继续追加:

- 5 个企业账号。
- 20 个求职者账号。
- 20 份简历。
- 60 个岗位。
- 100 条投递记录。

这批数据用于:

- 岗位列表分页。
- 企业“我的岗位”分页。
- 企业“收到的投递”分页。
- 热门岗位 / 热门公司缓存。
- 管理员用户、岗位、投递统计。
- 前端 ECharts 图表展示。

批量账号规则:

- 企业账号: `company_1` 到 `company_5`, 密码均为 `enterprise123`。
- 求职者账号: `candidate_01` 到 `candidate_20`, 密码均为 `user123`。

## 6. Redis 演示状态

SQL 只初始化 MySQL, 不初始化 Redis。

热门岗位和热门公司缓存可以由后端从 MySQL 自动重建。干净 Redis 环境下, 首次访问热点接口后会重新写入:

```text
airesume:cache:hot:jobs
airesume:cache:hot:companies
```

如果本地演示时希望 Redis bitmap 与种子数据中的“今日签到”流水保持一致, 可以执行:

```bash
YM=$(date +%Y%m)
OFFSET=$(($(date +%d) - 1))
docker exec ai-resume-redis redis-cli setbit "airesume:sign:3:$YM" "$OFFSET" 1
docker exec ai-resume-redis redis-cli setbit "airesume:sign:5:$YM" "$OFFSET" 1
```

注意:

- Redis bitmap 只是签到状态的快速表示。
- `credit_transaction` 才是签到发币和 AI 币余额变动的最终凭证。

## 7. 验证 SQL

导入后可以执行以下 SQL 检查演示数据:

```sql
SELECT role, COUNT(*) AS cnt FROM app_user GROUP BY role;

SELECT COUNT(*) AS resumes FROM resume;

SELECT COUNT(*) AS resume_scores FROM resume_score;

SELECT COUNT(*) AS resume_optimizes FROM resume_optimize;

SELECT status, COUNT(*) AS cnt FROM job GROUP BY status;

SELECT status, COUNT(*) AS cnt FROM application GROUP BY status;

SELECT operation, status, COUNT(*) AS cnt, SUM(total_tokens) AS tokens, SUM(credit_cost) AS credits
FROM llm_call_log
GROUP BY operation, status
ORDER BY operation, status;

SELECT type, SUM(change_amount) AS amount, COUNT(*) AS cnt
FROM credit_transaction
GROUP BY type
ORDER BY type;
```

预期核心结果:

```text
ADMIN: 1
ENTERPRISE: 2
USER: 3
resumes: 5
resume_scores: 3
resume_optimizes: 2
jobs: 6 OPEN, 1 CLOSED
applications: 3 PENDING, 2 VIEWED, 2 ACCEPTED, 1 REJECTED
```

导入 `bulk_demo_data.sql` 后, 预期额外具备:

```text
ENTERPRISE: 7
USER: 23
jobs: 67
applications: 108
默认企业 enterprise: 28 个岗位, 52 条投递
```

## 8. P7 上云注意事项

- 数据库名统一使用 `db_airesume`。
- SQL 文件中不要写入任何真实密钥。
- `DEEPSEEK_API_KEY` 通过环境变量或 Kubernetes Secret 注入。
- 本地上传目录 `uploads/` 已被 Git 忽略。
- K3s 上云时, 文件上传存储第一版可使用单副本 + PVC。
- 如果时间允许, 后续可迁移到 OSS / MinIO, 让后端容器保持无状态。
- `data.sql` 会重置演示数据, 不要在保存真实用户数据的生产库执行。
- `bulk_demo_data.sql` 会清理并重建 ID 范围内的批量演示数据, 也只建议用于演示环境。

## 9. 推荐交接方式

P7 上云前建议按下面顺序准备:

1. 确认线上 MySQL 已创建或可创建 `db_airesume`。
2. 导入 `schema.sql`。
3. 导入 `data.sql`。
4. 如需分页和图表演示数据, 继续导入 `bulk_demo_data.sql`。
5. 创建 Kubernetes Secret, 注入数据库密码和 `DEEPSEEK_API_KEY`。
6. 启动后端服务。
7. 访问 `/api/health` 验证 MySQL / Redis 连通性。
8. 登录 `admin/admin123`, 检查管理员大盘数据。
9. 登录 `user/user123`, 检查简历、岗位、投递、AI 币、签到数据。
