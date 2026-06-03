USE db_airesume;

-- Reset demo data to keep local testing and P7 handoff reproducible.
-- This script is intended for demo initialization, not for preserving production data.
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM llm_call_log;
DELETE FROM credit_transaction;
DELETE FROM job_match;
DELETE FROM application;
DELETE FROM job;
DELETE FROM resume_optimize;
DELETE FROM resume_score;
DELETE FROM resume_file;
DELETE FROM resume;
DELETE FROM app_user;
SET FOREIGN_KEY_CHECKS = 1;

-- Demo accounts
-- admin/admin123, enterprise/enterprise123, user/user123.
-- Additional USER accounts reuse user123; additional ENTERPRISE accounts reuse enterprise123.
INSERT INTO app_user (id, username, phone, password_hash, role, nick_name, credit_balance, status)
VALUES
  (1, 'admin', NULL, '$2a$10$/Jr.s1Zgh1bYxI1eoldSn.KZKTkqSClWW3uS5Y7kbEa8JIjGCj/lm', 'ADMIN', '系统管理员', 100, 'ACTIVE'),
  (2, 'enterprise', NULL, '$2a$10$OhiV/lP2cTgQKeVZNM7uxem.Cw7tDlX5WDF.mcg.jShcmEvctrO06', 'ENTERPRISE', '云栈科技', 50, 'ACTIVE'),
  (3, 'user', NULL, '$2a$10$TixSNbc1FAR4KkKUXdChW.VthBhCXC/EY4MjBwbSfmlFpX/1Xkaoa', 'USER', '演示求职者', 26, 'ACTIVE'),
  (4, 'user_li', NULL, '$2a$10$TixSNbc1FAR4KkKUXdChW.VthBhCXC/EY4MjBwbSfmlFpX/1Xkaoa', 'USER', '李明', 18, 'ACTIVE'),
  (5, 'user_chen', NULL, '$2a$10$TixSNbc1FAR4KkKUXdChW.VthBhCXC/EY4MjBwbSfmlFpX/1Xkaoa', 'USER', '陈雨', 28, 'ACTIVE'),
  (6, 'enterprise_ops', NULL, '$2a$10$OhiV/lP2cTgQKeVZNM7uxem.Cw7tDlX5WDF.mcg.jShcmEvctrO06', 'ENTERPRISE', '星云平台', 50, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  nick_name = VALUES(nick_name),
  credit_balance = VALUES(credit_balance),
  status = VALUES(status);

-- Resumes
INSERT INTO resume (id, user_id, title, content_md, created_at, updated_at)
VALUES
  (
    1,
    3,
    '运维开发实习简历',
    '## 求职方向\n运维开发 / 云原生平台实习\n\n## 技能\n- 熟悉 Linux 常用命令、Shell 脚本、Docker Compose。\n- 使用 Spring Boot、MySQL、Redis 开发过 AI 简历系统。\n- 了解 Kubernetes、Prometheus、Grafana、Jenkins GitOps。\n\n## 项目\nAiResume: 使用 Redis 承载验证码、登录态、签到 bitmap 和热点缓存; 记录 DeepSeek token、耗时和 AI 币消耗; 计划部署到 K3s。',
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    2,
    3,
    'Java 后端实习简历',
    '## 求职方向\nJava 后端开发实习\n\n## 技能\n- Spring Boot REST API、MyBatis-Plus、MySQL 表设计。\n- Redis 缓存、登录态、热点数据治理。\n- 具备 Docker 本地开发环境和接口联调经验。\n\n## 项目\n库存管理系统: 完成商品、入库、销售、库存预警模块。AiResume: 完成登录、CRUD、AI 调用和额度治理。',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    3,
    4,
    '后端测试实习简历',
    '## 求职方向\n后端开发 / 测试开发实习\n\n## 技能\n- Java、Spring Boot、MySQL、接口测试。\n- 熟悉 Postman、curl、基础 Linux。\n- 了解 Redis 缓存和常见数据结构。\n\n## 项目\n校园任务管理系统: 完成任务 CRUD、权限校验和统计接口。',
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    DATE_SUB(NOW(), INTERVAL 3 DAY)
  ),
  (
    4,
    5,
    '数据平台实习简历',
    '## 求职方向\n数据平台 / 运维开发实习\n\n## 技能\n- Python、SQL、Linux、Docker。\n- 了解日志采集、指标监控和 Grafana 可视化。\n- 能编写脚本完成数据清洗和定时任务。\n\n## 项目\n日志分析小工具: 解析 Nginx access log, 统计接口访问量和错误状态码。',
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    5,
    4,
    '云原生入门简历',
    '## 求职方向\n云原生平台实习\n\n## 技能\n- Docker、Kubernetes 基础资源对象。\n- Jenkins Pipeline 基础。\n- Prometheus 指标和 Grafana 面板使用。\n\n## 项目\n个人博客部署: 使用 Docker Compose 部署 Nginx、MySQL 和应用服务。',
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  )
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  title = VALUES(title),
  content_md = VALUES(content_md),
  updated_at = VALUES(updated_at);

-- Jobs
INSERT INTO job (id, enterprise_id, title, jd_content, tech_stack, location, status, created_at, updated_at)
VALUES
  (
    1,
    2,
    '运维开发实习生',
    '负责内部平台自动化、监控告警、CI/CD 与基础设施脚本开发; 参与 Redis、MySQL、Linux 服务日常维护; 协助编写部署文档和故障排查手册。',
    'Linux,Docker,Kubernetes,Redis,Spring Boot,Prometheus',
    '杭州',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 11 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    2,
    2,
    'Java 后端实习生',
    '参与 Spring Boot 业务接口开发、MySQL 表设计、Redis 缓存治理和接口联调; 需要具备基础 Java Web 开发能力。',
    'Java,Spring Boot,MySQL,Redis,REST API',
    '上海',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    3,
    2,
    '平台工程实习生',
    '参与内部 DevOps 平台、Kubernetes 发布流程和监控告警体系建设; 协助维护 Jenkins Pipeline 和应用部署清单。',
    'Kubernetes,Jenkins,Prometheus,Grafana,GitOps',
    '远程',
    'CLOSED',
    DATE_SUB(NOW(), INTERVAL 9 DAY),
    DATE_SUB(NOW(), INTERVAL 4 DAY)
  ),
  (
    4,
    6,
    'SRE 实习生',
    '参与线上服务稳定性建设, 维护 Prometheus / Grafana 看板, 跟进告警、容量和故障复盘。',
    'Linux,Prometheus,Grafana,Docker,Kubernetes',
    '北京',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    5,
    6,
    '可观测性平台实习生',
    '参与日志、指标和链路追踪平台建设, 负责仪表盘、告警规则和基础数据分析。',
    'Prometheus,Loki,Grafana,Java,SQL',
    '深圳',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    6,
    6,
    'DevOps 实习生',
    '参与 Jenkins CI/CD、容器镜像构建、K8s 应用发布和回滚流程维护。',
    'Jenkins,Docker,Kubernetes,Nginx,Shell',
    '广州',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 6 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    7,
    2,
    'Redis 运维实习生',
    '参与 Redis 实例巡检、慢查询分析、内存水位和淘汰策略治理, 编写自动化巡检脚本。',
    'Redis,Linux,Shell,MySQL,Prometheus',
    '杭州',
    'OPEN',
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  )
ON DUPLICATE KEY UPDATE
  enterprise_id = VALUES(enterprise_id),
  title = VALUES(title),
  jd_content = VALUES(jd_content),
  tech_stack = VALUES(tech_stack),
  location = VALUES(location),
  status = VALUES(status),
  updated_at = VALUES(updated_at);

-- Applications
INSERT INTO application (id, user_id, resume_id, job_id, status, remark, reviewed_by, reviewed_at, created_at, updated_at)
VALUES
  (1, 3, 1, 1, 'PENDING', '希望参与云原生和自动化方向实习。', NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (2, 3, 1, 4, 'VIEWED', '对 SRE 和监控体系很感兴趣。', 6, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (3, 4, 3, 1, 'ACCEPTED', '希望获得后端开发实习机会。', 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (4, 5, 4, 2, 'REJECTED', '希望参与 Java 后端和数据平台相关工作。', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (5, 3, 2, 2, 'VIEWED', '熟悉 Spring Boot 和 Redis, 可尽快到岗。', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (6, 4, 5, 5, 'PENDING', '希望参与监控和可观测性平台建设。', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (7, 5, 4, 6, 'PENDING', '有脚本和日志分析经验, 希望参与 DevOps 流程。', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (8, 4, 5, 4, 'ACCEPTED', '对云原生平台和 SRE 工作感兴趣。', 6, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  status = VALUES(status),
  remark = VALUES(remark),
  reviewed_by = VALUES(reviewed_by),
  reviewed_at = VALUES(reviewed_at),
  updated_at = VALUES(updated_at);

-- AI resume scores
INSERT INTO resume_score (
  id, resume_id, target_direction, overall_score, dimensions_json, suggestions,
  llm_model, prompt_tokens, completion_tokens, total_tokens, created_at
)
VALUES
  (
    1,
    1,
    '运维开发实习',
    86,
    '[{"name":"云原生匹配度","score":88,"comment":"项目包含 Docker、Redis、监控和 K3s 上线规划"},{"name":"后端工程能力","score":82,"comment":"具备 Spring Boot、MySQL 和接口开发经验"},{"name":"运维可观测性","score":90,"comment":"Prometheus/Grafana 和 Redis 运维叙事清晰"}]',
    '["补充线上部署后的资源限制和告警截图","把 Redis volatile-lru 和热点缓存策略写进项目亮点","增加一次故障排查或容量评估经历"]',
    'deepseek-chat',
    1160,
    520,
    1680,
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    2,
    2,
    'Java 后端实习',
    78,
    '[{"name":"Java 基础","score":76,"comment":"有 Spring Boot 项目, 但算法和 JVM 描述较少"},{"name":"数据库设计","score":80,"comment":"有 MySQL 表设计和业务 CRUD 经验"},{"name":"接口联调","score":82,"comment":"接口文档和本地调试链路较完整"}]',
    '["补充单元测试或接口测试经历","把项目中的异常处理和分页设计写得更明确","增加一次性能或慢 SQL 分析案例"]',
    'deepseek-chat',
    980,
    430,
    1410,
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    3,
    3,
    '后端开发实习',
    72,
    '[{"name":"后端开发","score":74,"comment":"具备 CRUD 和接口测试经历"},{"name":"项目完整度","score":70,"comment":"项目偏课程任务, 需要突出个人负责模块"},{"name":"工程化","score":68,"comment":"CI/CD 和部署经验较少"}]',
    '["补充接口压测或测试报告","将项目职责按模块拆分","增加部署环境和故障处理描述"]',
    'deepseek-chat',
    860,
    380,
    1240,
    DATE_SUB(NOW(), INTERVAL 3 DAY)
  )
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  target_direction = VALUES(target_direction),
  overall_score = VALUES(overall_score),
  dimensions_json = VALUES(dimensions_json),
  suggestions = VALUES(suggestions),
  llm_model = VALUES(llm_model),
  prompt_tokens = VALUES(prompt_tokens),
  completion_tokens = VALUES(completion_tokens),
  total_tokens = VALUES(total_tokens),
  created_at = VALUES(created_at);

-- AI resume optimization records
INSERT INTO resume_optimize (
  id, resume_id, target_direction, summary, optimized_bullets, rewrite_suggestions,
  llm_model, prompt_tokens, completion_tokens, total_tokens, latency_ms, created_at
)
VALUES
  (
    1,
    1,
    '运维开发实习',
    '简历方向清晰, 建议进一步突出 Redis 运维治理、监控闭环和 K3s 上线结果。',
    '["使用 Redis 承载验证码、登录态滑动续期、签到 bitmap 和热点缓存, 并配置 maxmemory 与 volatile-lru 淘汰策略。","记录 DeepSeek 调用 token、耗时、状态和 AI 币消耗, 为 Prometheus/Grafana 管理大盘提供数据基础。","基于 Docker Compose 搭建 MySQL/Redis 本地开发环境, 规划 K3s + Jenkins GitOps 上线流程。"]',
    '["把\\"熟悉 Redis\\"改为\\"使用 Redis 完成登录态、签到和热点缓存治理\\"。","把\\"了解监控\\"改为\\"接入 Actuator 指标并规划 Grafana 看板展示 JVM、HTTP、Redis 和 LLM token 指标\\"。"]',
    'deepseek-chat',
    1210,
    620,
    1830,
    1460,
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    2,
    2,
    'Java 后端实习',
    'Java 后端方向建议强化接口设计、事务边界和数据一致性表达。',
    '["使用 MyBatis-Plus 实现简历、岗位和投递 CRUD, 并通过角色校验限制资源访问边界。","AI 调用成功后在同一业务事务中写入结果、扣减 AI 币并记录额度流水。"]',
    '["补充 GlobalExceptionHandler、ApiResponse 和 traceId 的统一接口规范。","增加 MySQL 条件更新防止 AI 币并发扣成负数的说明。"]',
    'deepseek-chat',
    1040,
    510,
    1550,
    1320,
    NOW()
  )
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  target_direction = VALUES(target_direction),
  summary = VALUES(summary),
  optimized_bullets = VALUES(optimized_bullets),
  rewrite_suggestions = VALUES(rewrite_suggestions),
  llm_model = VALUES(llm_model),
  prompt_tokens = VALUES(prompt_tokens),
  completion_tokens = VALUES(completion_tokens),
  total_tokens = VALUES(total_tokens),
  latency_ms = VALUES(latency_ms),
  created_at = VALUES(created_at);

-- AI job matches
INSERT INTO job_match (id, resume_id, job_id, match_score, strengths, gaps, suggestions, created_at)
VALUES
  (
    1,
    1,
    1,
    88,
    '["简历中有 Redis、监控、Docker 和 Spring Boot 项目实践","目标方向与岗位职责高度一致","项目具备 AI 成本治理和上线规划"]',
    '["实际线上故障处理案例仍偏少","Kubernetes 生产排障经验需要补充"]',
    '["补充 K3s 部署截图和资源限制配置","增加 Redis 慢查询或内存淘汰策略的验证记录"]',
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    2,
    1,
    4,
    82,
    '["具备 Prometheus/Grafana 规划和 Redis 运维叙事","有 AI 服务监控和 token 成本记录意识"]',
    '["SRE 故障复盘经历不足","告警规则和 on-call 流程经验较少"]',
    '["补充服务健康检查、告警阈值和故障演练说明"]',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    3,
    2,
    2,
    80,
    '["Spring Boot、MySQL、Redis 与岗位技术栈匹配","有接口文档和本地 Docker 调试经验"]',
    '["Java 基础深度和测试覆盖还可加强"]',
    '["补充接口测试、事务和分页实现细节"]',
    NOW()
  )
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  job_id = VALUES(job_id),
  match_score = VALUES(match_score),
  strengths = VALUES(strengths),
  gaps = VALUES(gaps),
  suggestions = VALUES(suggestions),
  created_at = VALUES(created_at);

-- LLM call logs for admin dashboard and token trend charts
INSERT INTO llm_call_log (
  id, user_id, operation, provider, model, prompt_tokens, completion_tokens,
  total_tokens, credit_cost, latency_ms, status, error_message, created_at
)
VALUES
  (1, 3, 'RESUME_SCORE', 'DEEPSEEK', 'deepseek-chat', 1160, 520, 1680, 1, 1380, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (2, 3, 'JOB_MATCH', 'DEEPSEEK', 'deepseek-chat', 980, 410, 1390, 1, 1210, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (3, 4, 'RESUME_SCORE', 'DEEPSEEK', 'deepseek-chat', 860, 380, 1240, 1, 1120, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (4, 5, 'RESUME_OPTIMIZE', 'DEEPSEEK', 'deepseek-chat', 1050, 560, 1610, 2, 1560, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (5, 3, 'RESUME_OPTIMIZE', 'DEEPSEEK', 'deepseek-chat', 1210, 620, 1830, 2, 1460, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (6, 4, 'JOB_MATCH', 'DEEPSEEK', 'deepseek-chat', 920, 360, 1280, 1, 1180, 'SUCCESS', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (7, 5, 'JOB_MATCH', 'DEEPSEEK', 'deepseek-chat', 900, 340, 1240, 0, 1090, 'FAILED', 'ai response is not valid JSON', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (8, 3, 'RESUME_SCORE', 'DEEPSEEK', 'deepseek-chat', 980, 430, 1410, 1, 1250, 'SUCCESS', NULL, NOW()),
  (9, 3, 'RESUME_OPTIMIZE', 'DEEPSEEK', 'deepseek-chat', 1040, 510, 1550, 2, 1320, 'SUCCESS', NULL, NOW()),
  (10, 3, 'JOB_MATCH', 'DEEPSEEK', 'deepseek-chat', 940, 390, 1330, 1, 1190, 'SUCCESS', NULL, NOW())
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  operation = VALUES(operation),
  provider = VALUES(provider),
  model = VALUES(model),
  prompt_tokens = VALUES(prompt_tokens),
  completion_tokens = VALUES(completion_tokens),
  total_tokens = VALUES(total_tokens),
  credit_cost = VALUES(credit_cost),
  latency_ms = VALUES(latency_ms),
  status = VALUES(status),
  error_message = VALUES(error_message),
  created_at = VALUES(created_at);

-- Credit transactions. Balances align with seeded app_user.credit_balance values.
INSERT INTO credit_transaction (
  id, user_id, change_amount, type, balance_after, ref_type, ref_id, remark, created_at
)
VALUES
  (1, 3, 20, 'REGISTER_BONUS', 20, NULL, NULL, 'seed initial credit', DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (2, 4, 20, 'REGISTER_BONUS', 20, NULL, NULL, 'seed initial credit', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (3, 5, 20, 'REGISTER_BONUS', 20, NULL, NULL, 'seed initial credit', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (4, 3, 10, 'ADMIN_GRANT', 30, 'ADMIN_GRANT', 1, '演示环境补充额度', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (5, 3, -1, 'AI_RESUME_SCORE', 29, 'LLM_CALL_LOG', 1, 'resume score', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (6, 3, -1, 'AI_JOB_MATCH', 28, 'LLM_CALL_LOG', 2, 'job match', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (7, 4, -1, 'AI_RESUME_SCORE', 19, 'LLM_CALL_LOG', 3, 'resume score', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (8, 5, 10, 'ADMIN_GRANT', 30, 'ADMIN_GRANT', 1, '管理员演示发放', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (9, 5, -2, 'AI_RESUME_OPTIMIZE', 28, 'LLM_CALL_LOG', 4, 'resume optimize', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (10, 3, -2, 'AI_RESUME_OPTIMIZE', 26, 'LLM_CALL_LOG', 5, 'resume optimize', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (11, 4, -1, 'AI_JOB_MATCH', 18, 'LLM_CALL_LOG', 6, 'job match', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (12, 3, 1, 'CHECK_IN', 27, 'CHECK_IN', NULL, 'daily check-in reward', NOW()),
  (13, 3, -1, 'AI_RESUME_SCORE', 26, 'LLM_CALL_LOG', 8, 'resume score', NOW()),
  (14, 5, 1, 'CHECK_IN', 29, 'CHECK_IN', NULL, 'daily check-in reward', NOW()),
  (15, 5, -1, 'AI_JOB_MATCH', 28, 'LLM_CALL_LOG', 10, 'job match', NOW())
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  change_amount = VALUES(change_amount),
  type = VALUES(type),
  balance_after = VALUES(balance_after),
  ref_type = VALUES(ref_type),
  ref_id = VALUES(ref_id),
  remark = VALUES(remark),
  created_at = VALUES(created_at);
