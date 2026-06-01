# AiResume 接口与拦截器文档

> 本文档记录当前项目已经实现的接口、拦截器、Redis key 和核心调用流程。后续新增 P2/P3 接口时继续在这里维护。

## 1. 全局约定

### 1.1 基础地址

本地后端默认端口:

```text
http://localhost:8080
```

### 1.2 统一响应结构

所有业务接口统一返回 `ApiResponse<T>`:

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "请求追踪 ID"
}
```

字段说明:

```text
code: 业务结果码, 比如 OK / BIZ_ERROR / UNAUTHORIZED / FORBIDDEN / SYSTEM_ERROR
message: 响应消息
data: 真实业务数据
traceId: 本次请求追踪 ID, 便于根据日志排查问题
```

### 1.3 登录请求头

登录后访问需要登录的接口, 前端传:

```http
Authorization: Bearer {token}
```

后端也兼容纯 token:

```http
Authorization: {token}
```

## 2. 拦截器链路

当前拦截器在 `WebMvcConfig` 中注册, 顺序如下:

```text
RefreshTokenInterceptor order=0
AuthInterceptor         order=1
```

也就是说, 每个请求会先尝试恢复登录态, 再判断是否必须登录。

### 2.1 RefreshTokenInterceptor

它拦截所有请求:

```text
/**
```

它做:

```text
读取 Authorization
兼容 Bearer token 和纯 token
Redis 查 airesume:login:token:{token}
查不到就放行
查到就构造 LoginUser
保存到 UserHolder
刷新 Redis token TTL 到 30 分钟
请求结束后 remove ThreadLocal
```

注意:

```text
它不负责拦截未登录, 只负责“有 token 就恢复登录态”。
```

### 2.2 AuthInterceptor

它拦截:

```text
/api/**
```

它放行:

```text
/api/auth/captcha
/api/auth/register
/api/auth/login
/api/health
/api/dev/**
/actuator/**
```

它做:

```text
从 UserHolder 获取当前用户
有用户就放行
没有用户就返回 401 UNAUTHORIZED
```

注意:

```text
UserHolder 里的用户不是 AuthInterceptor 自己查出来的,
而是前面的 RefreshTokenInterceptor 从 Redis 恢复出来的。
```

## 3. Redis Key

### 3.1 图形验证码

```text
key: airesume:login:captcha:{uuid}
type: String
value: 小写验证码答案
ttl: 2 minutes
```

用途:

```text
登录 / 注册前做人机校验。
验证码校验后会删除该 key, 不允许重复使用。
```

### 3.2 登录 token

```text
key: airesume:login:token:{token}
type: Hash
ttl: 30 minutes
```

Hash 字段:

```text
userId
username
role
nickName
creditBalance
```

用途:

```text
保存登录态。
每次请求带 token 时, RefreshTokenInterceptor 会刷新 TTL 到 30 分钟。
```

## 4. 认证接口

### 4.1 获取验证码

```http
GET /api/auth/captcha
```

是否需要登录:

```text
否
```

响应 data:

```json
{
  "uuid": "captcha uuid",
  "imageBase64": "data:image/png;base64,..."
}
```

它做:

```text
生成 uuid
使用 Hutool Captcha 生成图片验证码
验证码答案转小写
写入 Redis: airesume:login:captcha:{uuid}
设置 TTL 2 分钟
返回 uuid 和 base64 图片
```

### 4.2 注册

```http
POST /api/auth/register
```

是否需要登录:

```text
否
```

请求体:

```json
{
  "username": "user1",
  "password": "password123",
  "nickName": "用户1",
  "role": "USER",
  "captchaUuid": "...",
  "captchaCode": "abcd"
}
```

它做:

```text
校验验证码
删除验证码 Redis key
校验 role 只能是 USER 或 ENTERPRISE
不允许注册 ADMIN
根据 username 查询 app_user, 保证用户名唯一
使用 Hutool BCrypt 加密密码
插入 app_user
注册默认赠送 creditBalance=20
```

成功响应 data:

```json
null
```

常见失败:

```text
验证码过期: BIZ_ERROR / captcha expired
验证码错误: BIZ_ERROR / captcha is incorrect
用户名已存在: BIZ_ERROR / username already exists
注册 ADMIN: FORBIDDEN / admin registration is not allowed
```

### 4.3 登录

```http
POST /api/auth/login
```

是否需要登录:

```text
否
```

请求体:

```json
{
  "username": "user",
  "password": "user123",
  "captchaUuid": "...",
  "captchaCode": "abcd"
}
```

它做:

```text
校验验证码
删除验证码 Redis key
根据 username 查询 app_user
检查用户状态必须是 ACTIVE
使用 Hutool BCrypt 校验密码
生成 token
写入 Redis Hash: airesume:login:token:{token}
设置 token TTL 30 分钟
返回 token 和用户信息
```

响应 data:

```json
{
  "token": "...",
  "userId": 3,
  "username": "user",
  "role": "USER",
  "nickName": "演示求职者",
  "creditBalance": 20
}
```

常见失败:

```text
验证码过期: BIZ_ERROR / captcha expired
验证码错误: BIZ_ERROR / captcha is incorrect
用户名或密码错误: BIZ_ERROR / username or password is incorrect
用户被禁用: FORBIDDEN / user is disabled
```

### 4.4 当前登录用户

```http
GET /api/auth/me
```

是否需要登录:

```text
是
```

请求头:

```http
Authorization: Bearer {token}
```

它做:

```text
请求先经过 RefreshTokenInterceptor
RefreshTokenInterceptor 根据 token 从 Redis 恢复 LoginUser
AuthInterceptor 判断 UserHolder 中存在用户后放行
Controller 从 UserHolder 读取当前用户
返回当前用户信息
```

响应 data:

```json
{
  "token": "...",
  "userId": 3,
  "username": "user",
  "role": "USER",
  "nickName": "演示求职者",
  "creditBalance": 20
}
```

常见失败:

```text
未带 token: UNAUTHORIZED / unauthorized
token 过期: UNAUTHORIZED / unauthorized
Redis 中 token 被删除: UNAUTHORIZED / unauthorized
```

### 4.5 退出登录

```http
POST /api/auth/logout
```

是否需要登录:

```text
是
```

请求头:

```http
Authorization: Bearer {token}
```

它做:

```text
从 Authorization 读取 token
兼容 Bearer token 和纯 token
删除 Redis: airesume:login:token:{token}
清理当前请求 UserHolder
```

成功响应 data:

```json
null
```

## 5. 健康检查接口

### 5.1 服务健康检查

```http
GET /api/health
```

是否需要登录:

```text
否
```

它做:

```text
MySQL 执行 SELECT 1
Redis 执行 PING
返回服务、MySQL、Redis 状态
```

响应 data:

```json
{
  "service": "ai-resume",
  "mysql": "UP",
  "redis": "UP"
}
```

## 6. 开发测试接口

### 6.1 DeepSeek 连通性测试

```http
GET /api/dev/llm/hello
```

是否需要登录:

```text
否
```

它做:

```text
调用 DeepSeekClient.chat()
向 DeepSeek /chat/completions 发送一条测试 prompt
返回模型回复内容
```

响应 data:

```json
{
  "message": "DeepSeek 返回内容"
}
```

常见失败:

```text
未配置 DEEPSEEK_API_KEY: BIZ_ERROR / DEEPSEEK_API_KEY is not configured
DeepSeek HTTP 错误: BIZ_ERROR / DeepSeek request failed: HTTP xxx
DeepSeek 返回空内容: BIZ_ERROR / DeepSeek returned empty content
```

## 7. P2 业务接口

### 7.1 分页结构

P2 列表接口统一支持:

```http
?page=1&size=10
```

说明:

```text
page 默认 1
size 默认 10
size 最大 100
```

分页响应 data:

```json
{
  "records": [],
  "page": 1,
  "size": 10,
  "total": 0,
  "pages": 0
}
```

### 7.2 简历接口

#### 创建简历

```http
POST /api/resumes
```

权限:

```text
USER
```

请求体:

```json
{
  "title": "运维开发实习简历",
  "contentMd": "熟悉 Linux、Docker、Kubernetes、Spring Boot、Redis。"
}
```

#### 我的简历列表

```http
GET /api/resumes?page=1&size=10
```

权限:

```text
USER
```

#### 简历详情

```http
GET /api/resumes/{id}
```

权限:

```text
USER, 只能查看自己的简历
```

#### 更新简历

```http
PUT /api/resumes/{id}
```

权限:

```text
USER, 只能更新自己的简历
```

请求体同创建简历。

#### 删除简历

```http
DELETE /api/resumes/{id}
```

权限:

```text
USER, 只能删除自己的简历
```

### 7.3 岗位接口

#### 发布岗位

```http
POST /api/jobs
```

权限:

```text
ENTERPRISE
```

请求体:

```json
{
  "title": "运维开发实习生",
  "jdContent": "负责内部平台自动化、监控告警、CI/CD 与基础设施脚本开发。",
  "techStack": "Linux,Docker,Kubernetes,Redis,Spring Boot",
  "location": "杭州"
}
```

#### 开放岗位列表

```http
GET /api/jobs?page=1&size=10
```

权限:

```text
登录用户
```

说明:

```text
只返回 OPEN 状态岗位。
```

#### 我的岗位列表

```http
GET /api/jobs/mine?page=1&size=10
```

权限:

```text
ENTERPRISE
```

说明:

```text
返回当前企业自己的 OPEN / CLOSED 岗位。
```

#### 岗位详情

```http
GET /api/jobs/{id}
```

权限:

```text
登录用户
```

可见性:

```text
普通用户只能查看 OPEN 岗位。
企业可以查看自己发布的 OPEN / CLOSED 岗位。
```

#### 更新岗位

```http
PUT /api/jobs/{id}
```

权限:

```text
ENTERPRISE, 只能更新自己的岗位
```

请求体同发布岗位。

#### 关闭岗位

```http
PATCH /api/jobs/{id}/close
```

权限:

```text
ENTERPRISE, 只能关闭自己的岗位
```

说明:

```text
将 status 更新为 CLOSED, 关闭后不允许继续投递。
```

### 7.4 投递接口

投递状态:

```text
PENDING
VIEWED
REJECTED
ACCEPTED
```

#### 发起投递

```http
POST /api/applications
```

权限:

```text
USER
```

请求体:

```json
{
  "resumeId": 1,
  "jobId": 1,
  "remark": "希望参与云原生和自动化方向实习。"
}
```

规则:

```text
只能使用自己的简历投递。
只能投递 OPEN 岗位。
同一用户对同一岗位只能投递一次。
新投递状态为 PENDING。
```

#### 我的投递

```http
GET /api/applications/my?page=1&size=10
```

权限:

```text
USER
```

#### 收到的投递

```http
GET /api/applications/received?page=1&size=10
```

权限:

```text
ENTERPRISE
```

说明:

```text
只返回投递到当前企业岗位的申请。
```

#### 审核投递

```http
PATCH /api/applications/{id}/status
```

权限:

```text
ENTERPRISE, 只能审核投递到自己岗位的申请
```

请求体:

```json
{
  "status": "ACCEPTED",
  "remark": "简历匹配度较高，进入后续面试。"
}
```

规则:

```text
status 只能更新为 VIEWED / REJECTED / ACCEPTED。
REJECTED / ACCEPTED 为终态, 已终态申请不允许再次审核。
审核时写入 reviewedBy 和 reviewedAt。
```

## 8. AI 核心接口

P3 AI 核心接口统一使用 DeepSeek Chat Completions。

P3 规则:

```text
严格 JSON Prompt。
成功写业务表 + llm_call_log。
失败只写 llm_call_log, 不写业务结果。
credit_cost 固定为 0, AI 币扣减留到 P4。
不保存 raw_response。
```

### 8.1 简历评分

```http
POST /api/ai/resumes/{resumeId}/score
```

权限:

```text
USER, 只能评分自己的简历
```

请求体:

```json
{
  "targetDirection": "运维开发实习"
}
```

成功响应 data:

```json
{
  "id": 1,
  "resumeId": 1,
  "targetDirection": "运维开发实习",
  "overallScore": 82,
  "dimensions": [
    {
      "name": "项目匹配度",
      "score": 85,
      "comment": "项目经历与目标岗位较匹配"
    }
  ],
  "suggestions": [
    "补充 Prometheus/Grafana 监控指标截图"
  ],
  "llmModel": "deepseek-chat",
  "promptTokens": 100,
  "completionTokens": 300,
  "totalTokens": 400,
  "createdAt": "2026-06-01T12:00:00"
}
```

落库:

```text
resume_score
llm_call_log operation=RESUME_SCORE
```

### 8.2 简历优化建议

```http
POST /api/ai/resumes/{resumeId}/optimize
```

权限:

```text
USER, 只能优化自己的简历
```

请求体:

```json
{
  "targetDirection": "运维开发实习"
}
```

成功响应 data:

```json
{
  "summary": "整体方向正确, 但需要强化指标化表达。",
  "optimizedBullets": [
    "使用 Docker Compose 搭建 MySQL/Redis 本地环境, 支撑登录态与业务数据调试。"
  ],
  "rewriteSuggestions": [
    "将'了解 Redis'改为'使用 Redis 承载验证码、登录态和热点缓存'。"
  ],
  "llmModel": "deepseek-chat",
  "promptTokens": 100,
  "completionTokens": 300,
  "totalTokens": 400,
  "latencyMs": 1200
}
```

说明:

```text
P3 不修改 resume.content_md。
P3 不单独保存优化结果历史。
只写 llm_call_log operation=RESUME_OPTIMIZE。
```

### 8.3 岗位匹配

```http
POST /api/ai/jobs/{jobId}/match
```

权限:

```text
USER, 只能使用自己的简历匹配 OPEN 岗位
```

请求体:

```json
{
  "resumeId": 1
}
```

成功响应 data:

```json
{
  "id": 1,
  "resumeId": 1,
  "jobId": 1,
  "matchScore": 78,
  "strengths": [
    "简历中有 Spring Boot 和 Redis 项目经历"
  ],
  "gaps": [
    "缺少实际线上故障处理案例"
  ],
  "suggestions": [
    "补充 Jenkins 或 GitOps 发布流程"
  ],
  "createdAt": "2026-06-01T12:00:00"
}
```

落库:

```text
job_match
llm_call_log operation=JOB_MATCH
```

常见失败:

```text
非本人简历: FORBIDDEN / resume does not belong to current user
岗位不存在: BIZ_ERROR / job not found
岗位已关闭: BIZ_ERROR / job is closed
AI 返回非 JSON: BIZ_ERROR / ai response is not valid JSON
AI 返回缺字段: BIZ_ERROR / ai response missing field ...
```

## 9. Actuator 接口

Spring Boot Actuator 当前暴露:

```text
/actuator/health
/actuator/info
/actuator/prometheus
```

是否需要登录:

```text
否
```

用途:

```text
健康检查、基础信息、Prometheus 指标采集。
```

## 10. 请求流程示例

### 10.1 登录流程

```text
前端 GET /api/auth/captcha
后端生成验证码并写 Redis
前端展示验证码图片
前端 POST /api/auth/login
后端校验验证码
后端校验账号密码
后端生成 token 并写 Redis Hash
前端保存 token
```

### 10.2 带 token 访问 /me

```text
前端 GET /api/auth/me, Header 带 Authorization
RefreshTokenInterceptor 读取 token
RefreshTokenInterceptor 查 Redis 登录态
RefreshTokenInterceptor 构造 LoginUser 并保存到 UserHolder
RefreshTokenInterceptor 刷新 Redis TTL
AuthInterceptor 发现 UserHolder 中有用户, 放行
AuthController.me() 返回当前用户
请求结束后 RefreshTokenInterceptor 清理 ThreadLocal
```

### 10.3 未登录访问受保护接口

```text
前端 GET /api/auth/me, 不带 Authorization
RefreshTokenInterceptor 没有 token, 放行
AuthInterceptor 发现 UserHolder 中没有用户
返回 401 UNAUTHORIZED
Controller 不会执行
```

## 11. 当前种子账号

`sql/data.sql` 当前提供三个种子账号:

```text
admin / admin123
enterprise / enterprise123
user / user123
```

数据库中保存的是 BCrypt 后的 `password_hash`, 不是明文密码。
