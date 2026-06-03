# AiResume 本地调试手册

> 本文档用于本地开发、联调和演示前自检。当前本地 Docker Compose 只启动 MySQL 和 Redis, 后端用 Maven 在宿主机启动, 前端用 Vite 在宿主机启动。

## 1. 本地端口与账号

### 1.1 端口

| 服务 | 本地地址 |
|---|---|
| 后端 Spring Boot | `http://localhost:8080` |
| 前端 Vite | `http://localhost:5173` |
| MySQL Docker 映射端口 | `127.0.0.1:3307` |
| Redis Docker 映射端口 | `127.0.0.1:6380` |
| Prometheus P6 可观测性 | `http://localhost:9090` |
| Grafana P6 可观测性 | `http://localhost:3001` |

### 1.2 数据库账号

| 用途 | 用户名 | 密码 | 数据库 |
|---|---|---|---|
| 应用账号 | `airesume` | `airesume123` | `db_airesume` |
| root 账号 | `root` | `root123456` | `db_airesume` |

### 1.3 种子账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 求职者 | `user` | `user123` |
| 企业 | `enterprise` | `enterprise123` |
| 管理员 | `admin` | `admin123` |

## 2. 第一次启动

在项目根目录执行:

```bash
docker compose up -d
```

查看容器状态:

```bash
docker compose ps
```

预期看到:

```text
ai-resume-mysql   running / healthy
ai-resume-redis   running / healthy
```

首次启动 MySQL 会自动执行:

```text
sql/schema.sql
sql/data.sql
```

说明:

- `schema.sql` 创建库表。
- `data.sql` 写入三个种子账号和演示数据。
- SQL 只会在 MySQL 数据卷首次初始化时自动执行。

如果是已有本地数据卷, 新增表不会自动补建。P4.6 简历附件阶段需要确认存在:

```sql
SHOW TABLES LIKE 'resume_file';
```

如果不存在, 可以从 `sql/schema.sql` 中复制 `CREATE TABLE IF NOT EXISTS resume_file ...` 单独执行。

## 3. 环境变量

后端读取 DeepSeek Key:

```text
DEEPSEEK_API_KEY
```

本地可在 shell 中临时设置:

```bash
export DEEPSEEK_API_KEY=你的key
```

也可以把私有配置放到项目根目录 `.env`, 但 `.env` 已被 Git 忽略, 不要提交真实 key。

如果不配置 `DEEPSEEK_API_KEY`:

- 登录、简历 CRUD、岗位、投递、AI 币、管理员大盘仍可调试。
- AI 评分 / 优化 / 匹配会返回未配置 key 的业务错误。

## 4. 启动后端

项目根目录执行:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

本项目默认 `application.yml` 已设置:

```text
spring.profiles.active=local
```

所以也可以直接执行:

```bash
./mvnw spring-boot:run
```

本地 profile 会连接:

```text
MySQL: 127.0.0.1:3307/db_airesume
Redis: 127.0.0.1:6380
```

后端启动后验证:

```bash
curl http://localhost:8080/api/health
```

预期返回包含:

```json
{
  "code": "OK",
  "data": {
    "service": "ai-resume",
    "mysql": "UP",
    "redis": "UP"
  }
}
```

## 5. 启动前端

进入前端目录:

```bash
cd web
```

首次安装依赖:

```bash
npm install
```

启动开发服务器:

```bash
npm run dev
```

访问:

```text
http://localhost:5173
```

前端通过 Vite proxy 转发接口:

```text
/api -> http://localhost:8080
```

对应配置:

```text
web/vite.config.ts
```

## 6. 推荐联调启动顺序

每次本地联调建议按这个顺序:

```bash
# 1. 启动中间件
docker compose up -d

# 2. 确认 MySQL / Redis healthy
docker compose ps

# 3. 启动后端
./mvnw spring-boot:run

# 4. 新终端启动前端
cd web
npm run dev
```

然后打开:

```text
http://localhost:5173
```

## 7. 进入 Docker 容器调试

### 7.1 查看容器日志

查看 MySQL 日志:

```bash
docker logs -f ai-resume-mysql
```

查看 Redis 日志:

```bash
docker logs -f ai-resume-redis
```

查看 compose 全部日志:

```bash
docker compose logs -f
```

### 7.2 进入 MySQL 容器

进入 MySQL 命令行:

```bash
docker exec -it ai-resume-mysql mysql -uairesume -pairesume123 db_airesume
```

常用 SQL:

```sql
SHOW TABLES;
SELECT id, username, role, nick_name, credit_balance, status FROM app_user;
SELECT * FROM credit_transaction ORDER BY id DESC LIMIT 10;
SELECT id, user_id, operation, total_tokens, credit_cost, status, created_at FROM llm_call_log ORDER BY id DESC LIMIT 10;
SELECT id, title, status FROM job ORDER BY id;
SELECT id, user_id, resume_id, job_id, status FROM application ORDER BY id;
SELECT id, resume_id, user_id, original_name, file_size, file_ext, created_at FROM resume_file ORDER BY id DESC LIMIT 10;
```

退出:

```sql
exit
```

### 7.3 进入 Redis 容器

进入 Redis CLI:

```bash
docker exec -it ai-resume-redis redis-cli
```

常用命令:

```redis
PING
KEYS airesume:*
TTL airesume:login:token:你的token
HGETALL airesume:login:token:你的token
GET airesume:cache:hot:jobs
GET airesume:cache:hot:companies
```

查看签到 bitmap 示例:

```redis
KEYS airesume:sign:*
GETBIT airesume:sign:3:202606 1
```

说明:

- 签到 key 格式是 `airesume:sign:{userId}:{yyyyMM}`。
- bitmap offset 是 `dayOfMonth - 1`。

退出:

```redis
exit
```

## 8. 数据库重置

如果想清空本地 MySQL / Redis 数据并重新执行初始化 SQL:

```bash
docker compose down -v
docker compose up -d
```

注意:

```text
down -v 会删除 compose volume, 本地数据会丢失。
```

只重启容器但保留数据:

```bash
docker compose restart
```

## 9. 后端常用 Maven 命令

编译:

```bash
./mvnw -q -DskipTests compile
```

打包:

```bash
./mvnw -q -DskipTests package
```

运行测试:

```bash
./mvnw test
```

运行 jar:

```bash
java -jar target/ai-resume-0.0.1-SNAPSHOT.jar
```

说明:

- 运行 jar 时仍会读取 `application.yml` 和 `application-local.yml`。
- 如需 AI 功能, 运行前确保已设置 `DEEPSEEK_API_KEY`。

## 10. 前端常用命令

进入前端目录:

```bash
cd web
```

安装依赖:

```bash
npm install
```

开发启动:

```bash
npm run dev
```

生产构建:

```bash
npm run build
```

本地预览构建产物:

```bash
npm run preview
```

构建产物:

```text
web/dist/
```

## 11. 常用接口调试

健康检查:

```bash
curl http://localhost:8080/api/health
```

获取验证码:

```bash
curl http://localhost:8080/api/auth/captcha
```

浏览器推荐优先用前端页面调试登录, 因为验证码需要人工识别。

登录后接口需要请求头:

```http
Authorization: Bearer {token}
```

管理员接口示例:

```bash
curl -H "Authorization: Bearer 你的token" http://localhost:8080/api/admin/overview
```

USER AI 币余额:

```bash
curl -H "Authorization: Bearer 你的token" http://localhost:8080/api/credits/balance
```

## 12. IDE 调试后端

如果用 IntelliJ IDEA:

1. 打开项目根目录。
2. 确认 JDK 使用 Java 21。
3. 启动 Docker Compose 的 MySQL / Redis。
4. 找到 `AiResumeApplication`。
5. 使用 Debug 启动。
6. 需要 AI 功能时, 在 Run Configuration 里添加环境变量:

```text
DEEPSEEK_API_KEY=你的key
```

常用断点位置:

- 登录认证: `AuthController`, `AuthServiceImpl`。
- 登录态恢复: `RefreshTokenInterceptor`。
- 权限拦截: `AuthInterceptor`。
- AI 调用: `AiResumeServiceImpl`, `DeepSeekClient`。
- AI 币扣费: `CreditServiceImpl`。
- 管理员统计: `AdminDashboardServiceImpl`。

## 13. 前后端联调排查

### 13.1 前端报 401

可能原因:

- 未登录。
- token 过期。
- Redis 被重启或清空。
- 前端没有携带 `Authorization`。

处理:

- 重新登录。
- 查看浏览器 DevTools 的 Request Headers。
- 查看 Redis token:

```bash
docker exec -it ai-resume-redis redis-cli
KEYS airesume:login:token:*
```

### 13.2 前端报 403

可能原因:

- 当前角色访问了不属于自己的接口。

例子:

- USER 访问 `/api/admin/**`。
- ENTERPRISE 访问 `/api/resumes`。
- ADMIN 访问 USER 专属 AI 币接口。

处理:

- 切换正确种子账号。
- 检查前端路由守卫和菜单。

### 13.3 后端启动失败, MySQL 连接不上

检查容器:

```bash
docker compose ps
```

检查端口:

```bash
docker port ai-resume-mysql
```

检查日志:

```bash
docker logs ai-resume-mysql
```

确认 `application-local.yml` 中端口是:

```text
3307
```

### 13.4 后端启动失败, Redis 连接不上

检查 Redis:

```bash
docker exec -it ai-resume-redis redis-cli PING
```

预期:

```text
PONG
```

确认 `application-local.yml` 中端口是:

```text
6380
```

### 13.5 AI 调用失败

可能原因:

- 未设置 `DEEPSEEK_API_KEY`。
- key 错误或额度不足。
- 网络无法访问 DeepSeek。
- AI 返回非 JSON。
- 用户 AI 币余额不足。

处理:

- 确认环境变量:

```bash
echo $DEEPSEEK_API_KEY
```

- 查看后端日志里的 traceId。
- 查看 `llm_call_log`:

```sql
SELECT id, operation, status, error_message, created_at
FROM llm_call_log
ORDER BY id DESC
LIMIT 10;
```

### 13.6 前端接口代理不生效

检查:

- 后端是否在 `8080`。
- 前端是否从 `5173` 访问。
- `web/vite.config.ts` 是否配置:

```text
/api -> http://localhost:8080
```

如果前端直接请求 `http://localhost:8080`, 可能遇到 CORS 或环境差异。P5 本地开发建议统一走 `/api`。

## 14. 演示前自检清单

演示前建议依次检查:

```bash
docker compose ps
curl http://localhost:8080/api/health
./mvnw -q -DskipTests package
cd web
npm run build
```

浏览器演示:

- `user/user123` 登录。
- 创建或编辑简历。
- 签到领取 AI 币。
- 查看岗位并投递。
- 如配置了 DeepSeek Key, 演示 AI 评分 / 优化 / 匹配。
- `enterprise/enterprise123` 登录, 审核投递。
- `admin/admin123` 登录, 查看大盘, 给 USER 发 AI 币。

## 15. 当前本地架构

```text
Browser
  -> http://localhost:5173      Vite dev server
  -> /api proxy
  -> http://localhost:8080      Spring Boot backend
  -> 127.0.0.1:3307             MySQL in Docker
  -> 127.0.0.1:6380             Redis in Docker
```

当前 `compose.yaml` 只包含:

```text
ai-resume-mysql
ai-resume-redis
```

如果后续 P7 做上云或本地容器化前后端, 再新增 backend / frontend Dockerfile 和 compose 服务。

## 16. P6 本地可观测性

P6 只做最小可演示闭环:

```text
Spring Boot /actuator/prometheus
  -> Prometheus
  -> Grafana
```

### 16.1 启动监控

先启动业务依赖和后端:

```bash
docker compose up -d
./mvnw spring-boot:run
```

再启动 Prometheus 和 Grafana:

```bash
docker compose -f compose.monitoring.yaml up -d
```

访问:

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3001
```

Grafana 默认账号:

```text
admin / admin
```

首次登录如果 Grafana 要求修改密码, 按页面提示修改即可。P6 已预置 Prometheus 数据源和 Dashboard, 登录后进入:

```text
Dashboards -> AiResume -> AiResume LLM Observability
```

### 16.2 验证指标

验证 Actuator:

```bash
curl http://localhost:8080/actuator/prometheus
```

基础指标应包含:

```text
http_server_requests_seconds
jvm_memory_used_bytes
```

执行 AI 评分 / 优化 / 匹配后, 应包含:

```text
airesume_llm_calls_total
airesume_llm_tokens_total
airesume_llm_latency_seconds
```

Prometheus targets 页面:

```text
http://localhost:9090/targets
```

预期:

```text
ai-resume-backend UP
```

### 16.3 Grafana Dashboard

本地 Grafana 会自动加载:

```text
AiResume LLM Observability
```

包含:

- AI 调用总数。
- Token 总消耗。
- 平均耗时。
- 成功 / 失败次数。
- 各功能调用量。
- 调用速率趋势。

如果 Dashboard 没出现, 重新加载监控 compose:

```bash
docker compose -f compose.monitoring.yaml up -d
```

### 16.4 Grafana PromQL 示例

AI 调用量:

```promql
sum by (operation) (rate(airesume_llm_calls_total[5m]))
```

成功 / 失败趋势:

```promql
sum by (status) (rate(airesume_llm_calls_total[5m]))
```

Token 消耗:

```promql
sum by (operation) (rate(airesume_llm_tokens_total{type="total"}[5m]))
```

平均耗时:

```promql
sum by (operation) (rate(airesume_llm_latency_seconds_sum[5m]))
/
sum by (operation) (rate(airesume_llm_latency_seconds_count[5m]))
```

JVM 内存:

```promql
sum by (area) (jvm_memory_used_bytes)
```

HTTP 请求量:

```promql
sum by (uri, method) (rate(http_server_requests_seconds_count[5m]))
```

### 16.5 P7 上云复用方式

P7 不新增第二套 Prometheus / Grafana。后端镜像继续暴露:

```text
/actuator/prometheus
```

云上复用 Cloud-Ops-Hub 已有 Prometheus 抓取 AiResume 后端服务, 再在现有 Grafana 中导入或重建同样的面板。
