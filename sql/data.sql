USE db_airesume;

INSERT INTO app_user (id, username, phone, password_hash, role, nick_name, credit_balance, status)
VALUES
  (1, 'admin', NULL, '$2a$10$/Jr.s1Zgh1bYxI1eoldSn.KZKTkqSClWW3uS5Y7kbEa8JIjGCj/lm', 'ADMIN', '系统管理员', 100, 'ACTIVE'),
  (2, 'enterprise', NULL, '$2a$10$OhiV/lP2cTgQKeVZNM7uxem.Cw7tDlX5WDF.mcg.jShcmEvctrO06', 'ENTERPRISE', '演示企业', 50, 'ACTIVE'),
  (3, 'user', NULL, '$2a$10$TixSNbc1FAR4KkKUXdChW.VthBhCXC/EY4MjBwbSfmlFpX/1Xkaoa', 'USER', '演示求职者', 20, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  nick_name = VALUES(nick_name),
  credit_balance = VALUES(credit_balance),
  status = VALUES(status);

INSERT INTO resume (id, user_id, title, content_md)
VALUES
  (1, 3, '运维开发实习简历', '熟悉 Linux、Docker、Kubernetes、Spring Boot、Redis, 具备云原生项目实践。')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content_md = VALUES(content_md);

INSERT INTO job (id, enterprise_id, title, jd_content, tech_stack, location, status)
VALUES
  (1, 2, '运维开发实习生', '负责内部平台自动化、监控告警、CI/CD 与基础设施脚本开发。', 'Linux,Docker,Kubernetes,Redis,Spring Boot', '杭州', 'OPEN'),
  (2, 2, 'Java 后端实习生', '参与 Spring Boot 业务接口开发、MySQL 表设计、Redis 缓存治理和接口联调。', 'Java,Spring Boot,MySQL,Redis', '上海', 'OPEN'),
  (3, 2, '平台工程实习生', '参与内部 DevOps 平台、Kubernetes 发布流程和监控告警体系建设。', 'Kubernetes,Jenkins,Prometheus,Grafana', '远程', 'CLOSED')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  jd_content = VALUES(jd_content),
  tech_stack = VALUES(tech_stack),
  location = VALUES(location),
  status = VALUES(status);

INSERT INTO application (id, user_id, resume_id, job_id, status, remark, reviewed_by, reviewed_at)
VALUES
  (1, 3, 1, 1, 'PENDING', '希望参与云原生和自动化方向实习。', NULL, NULL)
ON DUPLICATE KEY UPDATE
  resume_id = VALUES(resume_id),
  status = VALUES(status),
  remark = VALUES(remark),
  reviewed_by = VALUES(reviewed_by),
  reviewed_at = VALUES(reviewed_at);
