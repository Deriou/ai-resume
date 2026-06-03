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
creditBalance 仅作为登录态快照, /api/auth/me 和额度接口以 MySQL 实时余额为准。
```

### 3.3 每日签到 bitmap

```text
key: airesume:sign:{userId}:{yyyyMM}
type: Bitmap
offset: dayOfMonth - 1
ttl: 不设置
```

用途:

```text
记录用户当月每日签到状态。
签到发币凭证以 MySQL credit_transaction 为准。
```

### 3.4 首页热点缓存

```text
key: airesume:cache:hot:jobs
type: String(JSON)
ttl: 正常结果 5 minutes + random(0~120 seconds), 空结果 30 seconds
```

```text
key: airesume:cache:hot:companies
type: String(JSON)
ttl: 正常结果 5 minutes + random(0~120 seconds), 空结果 30 seconds
```

用途:

```text
缓存首页热门岗位和热门公司。
应用启动后预热, 接口使用 Cache Aside, 写操作成功后删除缓存。
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
Controller 从 UserHolder 读取 userId
查询 MySQL app_user 获取实时 creditBalance
返回当前用户信息和最新余额
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

#### PDF 导入创建简历

```http
POST /api/resumes/import/pdf
Content-Type: multipart/form-data
```

权限:

```text
USER
```

表单字段:

```text
file: PDF 文件
title: 可选, 不填时使用 PDF 文件名
```

说明:

```text
第一版只支持可复制文字的 PDF, 不做 OCR。
后端使用 PDFBox 提取文本, 写入 resume.content_md。
导入成功后会自动把原 PDF 保存为该简历附件。
导入不调用 DeepSeek, 不扣 AI 币。
AI 评分、优化、岗位匹配仍然基于导入后的 resume.content_md。
```

常见失败:

```text
非 PDF 文件: BIZ_ERROR / only pdf import is supported
扫描件或文本过短: BIZ_ERROR / pdf text is too short, please use a text-based PDF
加密 PDF: BIZ_ERROR / encrypted pdf is not supported
解析失败: BIZ_ERROR / failed to parse pdf
```

#### 我的简历列表

```http
GET /api/resumes?page=1&size=10
```

权限:

```text
USER
```

#### 简历最近评分列表

```http
GET /api/resumes/scores/latest
```

权限:

```text
USER, 只返回当前用户自己简历的评分
```

说明:

```text
返回当前用户每份简历的最近一次 AI 评分摘要, 供前端在简历卡片上展示评分 badge 和 AI 建议。
数据来源于已落库的 resume_score 表, 不触发新的 DeepSeek 调用, 不扣 AI 币。
每份简历只取最近一次评分 (按 createdAt 倒序, 再按 id 倒序去重)。
从未评分的简历不会出现在列表中; 用户没有任何评分时返回空数组。
suggestions 取该次评分落库的优化建议数组, 解析失败时返回空数组。
```

响应 data:

```json
[
  {
    "resumeId": 1,
    "targetDirection": "运维开发实习",
    "overallScore": 82,
    "suggestions": [
      "补充 Prometheus/Grafana 监控指标截图"
    ],
    "scoredAt": "2026-06-01T12:00:00"
  }
]
```

#### 简历润色历史列表

```http
GET /api/resumes/{id}/optimizations
```

权限:

```text
USER, 只能查看自己简历的润色历史
```

说明:

```text
返回某份简历全部 AI 润色 (优化) 历史记录, 按 createdAt 倒序, 再按 id 倒序。
数据来源于 resume_optimize 表, 不触发新的 DeepSeek 调用, 不扣 AI 币。
该接口只读历史, 不修改 resume.content_md。
该简历从未润色时返回空数组。
```

响应 data:

```json
[
  {
    "id": 1,
    "resumeId": 1,
    "targetDirection": "运维开发实习",
    "summary": "整体方向正确, 但需要强化指标化表达。",
    "optimizedBullets": [
      "使用 Docker Compose 搭建 MySQL/Redis 本地环境, 支撑登录态与业务数据调试。"
    ],
    "rewriteSuggestions": [
      "将'了解 Redis'改为'使用 Redis 承载验证码、登录态和热点缓存'。"
    ],
    "llmModel": "deepseek-chat",
    "totalTokens": 400,
    "latencyMs": 1200,
    "createdAt": "2026-06-01T12:00:00"
  }
]
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
如果简历下存在附件, 需要先删除附件, 否则返回 BIZ_ERROR
```

### 7.2.1 简历附件接口

P4.6 普通简历附件只做上传、列表、下载、删除, 不做 PDF / DOC / DOCX 内容解析。P5 新增的 PDF 导入创建简历会解析 PDF 文本并写入 `resume.content_md`。AI 评分、优化、岗位匹配始终基于 `resume.content_md`。

文件规则:

```text
支持扩展名: pdf, doc, docx
单文件大小: 10MB
单份简历最多附件数: 3 个
MIME 白名单: application/pdf, application/msword,
application/vnd.openxmlformats-officedocument.wordprocessingml.document,
application/octet-stream
```

#### 上传附件

```http
POST /api/resumes/{resumeId}/files
Content-Type: multipart/form-data
```

权限:

```text
USER, 只能给自己的简历上传附件
```

表单字段:

```text
file
```

响应 data:

```json
{
  "id": 1,
  "resumeId": 1,
  "originalName": "谷强_运维开发实习.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576,
  "fileExt": "pdf",
  "createdAt": "2026-06-02T12:00:00"
}
```

#### 附件列表

```http
GET /api/resumes/{resumeId}/files
```

权限:

```text
USER, 只能查看自己的简历附件
```

响应 data:

```json
[
  {
    "id": 1,
    "resumeId": 1,
    "originalName": "谷强_运维开发实习.pdf",
    "contentType": "application/pdf",
    "fileSize": 1048576,
    "fileExt": "pdf",
    "createdAt": "2026-06-02T12:00:00"
  }
]
```

#### 下载附件

```http
GET /api/resume-files/{fileId}/download
```

权限:

```text
USER, 只能下载自己的附件
```

说明:

```text
下载接口直接返回文件流, 不包 ApiResponse。
响应头包含 Content-Disposition: attachment, 使用原始文件名下载。
```

#### 删除附件

```http
DELETE /api/resume-files/{fileId}
```

权限:

```text
USER, 只能删除自己的附件
```

说明:

```text
删除数据库元数据, 并尽力删除本地物理文件。
如果物理文件已经不存在, 元数据删除仍可成功。
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
返回字段包含 `resumeContentMd`, 企业可在审核前查看求职者投递的简历正文。
```

响应 records 示例:

```json
{
  "id": 1,
  "userId": 3,
  "resumeId": 1,
  "resumeTitle": "运维开发实习简历",
  "resumeContentMd": "熟悉 Linux、Docker、Kubernetes、Spring Boot、Redis。",
  "jobId": 1,
  "jobTitle": "运维开发实习生",
  "status": "PENDING",
  "remark": "希望参与云原生和自动化方向实习。",
  "reviewedBy": null,
  "reviewedAt": null,
  "createdAt": "2026-06-01T12:00:00",
  "updatedAt": "2026-06-01T12:00:00"
}
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

AI 核心接口统一使用 DeepSeek Chat Completions。

P4 后规则:

```text
严格 JSON Prompt。
AI 调用前检查 AI 币余额, 余额不足不调用 DeepSeek。
成功写业务表 + llm_call_log + credit_transaction。
成功后按功能固定扣费, 并更新 llm_call_log.credit_cost。
失败只写 llm_call_log, 不写业务结果。
AI 调用失败或 JSON 解析失败不扣费。
不保存 raw_response。
```

固定扣费:

```text
RESUME_SCORE: 1
RESUME_OPTIMIZE: 2
JOB_MATCH: 1
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
不修改 resume.content_md, 优化结果只作为建议, 是否采纳由用户自行编辑简历。
优化结果会落库到 resume_optimize 表, 保存润色历史, 可通过
GET /api/resumes/{id}/optimizations 查看 (P5 起新增, 取代早期"不保存优化历史"的设定)。
同时写 llm_call_log operation=RESUME_OPTIMIZE。
```

落库:

```text
resume_optimize
llm_call_log operation=RESUME_OPTIMIZE
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
余额不足: BIZ_ERROR / insufficient credit balance
AI 返回非 JSON: BIZ_ERROR / ai response is not valid JSON
AI 返回缺字段: BIZ_ERROR / ai response missing field ...
```

## 9. P4 额度与 Redis 强化接口

### 9.1 查询 AI 币余额

```http
GET /api/credits/balance
```

权限:

```text
USER
```

响应 data:

```json
{
  "balance": 20
}
```

说明:

```text
余额以 MySQL app_user.credit_balance 为准。
Redis 不保存 AI 币最终余额。
```

### 9.2 查询我的额度流水

```http
GET /api/credits/transactions?page=1&size=10
```

权限:

```text
USER
```

响应 data:

```json
{
  "records": [
    {
      "id": 1,
      "userId": 3,
      "changeAmount": -1,
      "type": "AI_RESUME_SCORE",
      "balanceAfter": 19,
      "refType": "LLM_CALL_LOG",
      "refId": 10,
      "remark": "resume score",
      "createdAt": "2026-06-01T12:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "pages": 1
}
```

### 9.3 今日签到状态

```http
GET /api/credits/check-in/today
```

权限:

```text
USER
```

响应 data:

```json
{
  "checkedIn": false,
  "date": "2026-06-01",
  "creditReward": 1,
  "balance": 20
}
```

说明:

```text
优先查询 MySQL 当天 CHECK_IN 流水。
MySQL 无流水时再查询 Redis bitmap。
前端根据 checkedIn 展示“签到领 1 AI 币”或“今日已签到”。
```

### 9.4 签到领 AI 币

```http
POST /api/credits/check-in
```

权限:

```text
USER
```

它做:

```text
查询今天是否已有 CHECK_IN 流水。
未签到则发放 1 AI 币。
写 credit_transaction type=CHECK_IN。
设置 Redis bitmap airesume:sign:{userId}:{yyyyMM} 对应日期 bit。
返回最新余额。
```

响应 data:

```json
{
  "checkedIn": true,
  "date": "2026-06-01",
  "creditReward": 1,
  "balance": 21
}
```

常见失败:

```text
重复签到: BIZ_ERROR / already checked in
```

### 9.5 热门岗位

```http
GET /api/hot/jobs
```

权限:

```text
登录用户
```

响应 data:

```json
[
  {
    "jobId": 1,
    "title": "运维开发实习生",
    "enterpriseId": 2,
    "location": "杭州",
    "techStack": "Linux,Docker,Kubernetes,Redis,Spring Boot",
    "applicationCount": 3
  }
]
```

排序:

```text
只统计 OPEN 岗位。
按投递数倒序, 再按岗位创建时间倒序。
```

缓存策略:

```text
应用启动后预热。
接口先查 Redis, miss 再查 MySQL 并回写 Redis。
空结果缓存 [] 30 秒。
正常结果缓存 5 分钟 + 0~120 秒随机 TTL。
```

### 9.6 热门公司

```http
GET /api/hot/companies
```

权限:

```text
登录用户
```

响应 data:

```json
[
  {
    "enterpriseId": 2,
    "enterpriseName": "演示企业",
    "openJobCount": 2,
    "applicationCount": 5
  }
]
```

排序:

```text
按收到投递数倒序, 再按开放岗位数倒序。
```

缓存失效:

```text
企业发布岗位、更新岗位、关闭岗位、用户投递成功后删除热门岗位 / 公司缓存。
Redis 只保存热点副本, 最终数据以 MySQL 为准。
```

## 10. P4.5 管理员接口

P4.5 管理员接口统一要求:

```text
role = ADMIN
```

说明:

```text
统计接口固定统计近 7 日数据。
管理员发放 AI 币只允许发给 USER 账号。
岗位和投递管理接口只读, 不做管理员强制审核或删除。
```

### 10.1 管理员业务概览

```http
GET /api/admin/overview
```

响应 data:

```json
{
  "totalUsers": 3,
  "userCount": 1,
  "enterpriseCount": 1,
  "adminCount": 1,
  "resumeCount": 10,
  "jobCount": 8,
  "openJobCount": 6,
  "applicationCount": 12,
  "todayAiCalls": 5,
  "todayTokens": 12000,
  "todayCreditCost": 7,
  "todayAvgLatencyMs": 1300,
  "todayAiFailures": 1
}
```

### 10.2 LLM 统计

```http
GET /api/admin/llm/summary
GET /api/admin/llm/daily
GET /api/admin/llm/operations
```

`summary` 响应 data:

```json
{
  "days": 7,
  "callCount": 10,
  "successCount": 9,
  "failureCount": 1,
  "failureRate": 0.1,
  "promptTokens": 2000,
  "completionTokens": 6000,
  "totalTokens": 8000,
  "avgLatencyMs": 1200,
  "creditCost": 12
}
```

`daily` 返回近 7 日数组:

```json
[
  {
    "date": "2026-06-01",
    "callCount": 3,
    "successCount": 3,
    "failureCount": 0,
    "totalTokens": 2400,
    "creditCost": 4,
    "avgLatencyMs": 1100
  }
]
```

`operations` 按功能聚合:

```json
[
  {
    "operation": "RESUME_SCORE",
    "callCount": 4,
    "totalTokens": 3200,
    "creditCost": 4,
    "avgLatencyMs": 1000
  }
]
```

### 10.3 AI 币统计

```http
GET /api/admin/credits/summary
GET /api/admin/credits/daily
GET /api/admin/credits/top-users?limit=5
```

`summary` 响应 data:

```json
{
  "days": 7,
  "grantedCredits": 20,
  "consumedCredits": 12,
  "checkInCredits": 3,
  "adminGrantCredits": 17,
  "resumeScoreCredits": 4,
  "resumeOptimizeCredits": 6,
  "jobMatchCredits": 2
}
```

`daily` 返回近 7 日数组:

```json
[
  {
    "date": "2026-06-01",
    "grantedCredits": 5,
    "consumedCredits": 3,
    "checkInCredits": 1,
    "adminGrantCredits": 4,
    "aiConsumedCredits": 3
  }
]
```

`top-users` 返回 AI 币消耗 TOP 用户, 同时带 LLM token 和调用次数:

```json
[
  {
    "userId": 3,
    "username": "user",
    "nickName": "演示求职者",
    "consumedCredits": 8,
    "totalTokens": 5000,
    "aiCallCount": 6
  }
]
```

### 10.4 管理员用户列表

```http
GET /api/admin/users?page=1&size=10&role=USER&status=ACTIVE&keyword=user
```

过滤条件:

```text
role 可选: USER / ENTERPRISE / ADMIN
status 可选: ACTIVE / DISABLED
keyword 可选: 匹配 username / nickName
```

响应 data:

```json
{
  "records": [
    {
      "id": 3,
      "username": "user",
      "role": "USER",
      "nickName": "演示求职者",
      "creditBalance": 20,
      "status": "ACTIVE",
      "createdAt": "2026-06-01T12:00:00",
      "updatedAt": "2026-06-01T12:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "pages": 1
}
```

### 10.5 管理员发放 AI 币

```http
POST /api/admin/users/{userId}/credits/grant
```

请求体:

```json
{
  "amount": 10,
  "remark": "演示环境补充额度"
}
```

规则:

```text
只能 ADMIN 调用。
目标账号必须是 USER。
写 credit_transaction.type = ADMIN_GRANT。
```

响应 data 为本次额度流水:

```json
{
  "id": 10,
  "userId": 3,
  "changeAmount": 10,
  "type": "ADMIN_GRANT",
  "balanceAfter": 30,
  "refType": "ADMIN_GRANT",
  "refId": null,
  "remark": "演示环境补充额度",
  "createdAt": "2026-06-01T12:00:00"
}
```

### 10.6 管理员岗位只读列表

岗位运营摘要:

```http
GET /api/admin/jobs/summary
```

响应 data:

```json
{
  "jobCount": 7,
  "openJobCount": 5,
  "closedJobCount": 2,
  "applicationCount": 4,
  "noApplicationJobCount": 4,
  "avgApplicationsPerJob": 0.57
}
```

岗位分页列表:

```http
GET /api/admin/jobs?page=1&size=10&status=OPEN&keyword=运维
```

响应 records:

```json
{
  "id": 1,
  "enterpriseId": 2,
  "enterpriseName": "演示企业",
  "title": "运维开发实习生",
  "techStack": "Linux,Docker,Kubernetes,Redis,Spring Boot",
  "location": "杭州",
  "status": "OPEN",
  "applicationCount": 3,
  "createdAt": "2026-06-01T12:00:00",
  "updatedAt": "2026-06-01T12:00:00"
}
```

### 10.7 管理员投递只读列表

投递漏斗摘要:

```http
GET /api/admin/applications/summary
```

响应 data:

```json
{
  "applicationCount": 4,
  "pendingCount": 1,
  "viewedCount": 1,
  "acceptedCount": 1,
  "rejectedCount": 1,
  "reviewedRate": 0.75
}
```

投递分页列表:

```http
GET /api/admin/applications?page=1&size=10&status=PENDING
```

响应 records:

```json
{
  "id": 1,
  "userId": 3,
  "username": "user",
  "nickName": "演示求职者",
  "resumeId": 1,
  "resumeTitle": "运维开发实习简历",
  "jobId": 1,
  "jobTitle": "运维开发实习生",
  "enterpriseId": 2,
  "enterpriseName": "演示企业",
  "status": "PENDING",
  "remark": "希望参与云原生和自动化方向实习。",
  "reviewedBy": null,
  "reviewedAt": null,
  "createdAt": "2026-06-01T12:00:00",
  "updatedAt": "2026-06-01T12:00:00"
}
```

## 11. Actuator 接口

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

## 12. 请求流程示例

### 12.1 登录流程

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

### 12.2 带 token 访问 /me

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

### 12.3 未登录访问受保护接口

```text
前端 GET /api/auth/me, 不带 Authorization
RefreshTokenInterceptor 没有 token, 放行
AuthInterceptor 发现 UserHolder 中没有用户
返回 401 UNAUTHORIZED
Controller 不会执行
```

## 13. 当前种子账号

`sql/data.sql` 当前提供三个种子账号:

```text
admin / admin123
enterprise / enterprise123
user / user123
```

数据库中保存的是 BCrypt 后的 `password_hash`, 不是明文密码。
