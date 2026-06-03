USE db_airesume;

-- Optional bulk demo data for pagination and dashboard testing.
-- Import after schema.sql and data.sql.
-- Generated IDs:
--   enterprises: 200-204
--   users:       300-319
--   resumes:     300-319
--   jobs:        200-259
--   applications:300-399

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM application WHERE id >= 300 OR user_id >= 300 OR job_id >= 200;
DELETE FROM job WHERE id >= 200;
DELETE FROM resume WHERE id >= 300 OR user_id >= 300;
DELETE FROM app_user WHERE id >= 200;
SET FOREIGN_KEY_CHECKS = 1;

DROP PROCEDURE IF EXISTS seed_bulk_demo;

DELIMITER //

CREATE PROCEDURE seed_bulk_demo()
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE user_id_val BIGINT;
  DECLARE resume_id_val BIGINT;
  DECLARE enterprise_id_val BIGINT;
  DECLARE job_id_val BIGINT;
  DECLARE app_id_val BIGINT;
  DECLARE job_idx INT;
  DECLARE user_idx INT;
  DECLARE status_val VARCHAR(32);
  DECLARE reviewed_by_val BIGINT;
  DECLARE reviewed_at_val DATETIME;
  DECLARE job_title_val VARCHAR(128);
  DECLARE jd_val TEXT;
  DECLARE stack_val VARCHAR(255);
  DECLARE location_val VARCHAR(128);

  -- More enterprise accounts, all using enterprise123.
  SET i = 0;
  WHILE i < 5 DO
    SET enterprise_id_val = 200 + i;

    INSERT INTO app_user (id, username, phone, password_hash, role, nick_name, credit_balance, status)
    VALUES (
      enterprise_id_val,
      CONCAT('company_', i + 1),
      NULL,
      '$2a$10$OhiV/lP2cTgQKeVZNM7uxem.Cw7tDlX5WDF.mcg.jShcmEvctrO06',
      'ENTERPRISE',
      CASE i
        WHEN 0 THEN '北辰云科'
        WHEN 1 THEN '青木数据'
        WHEN 2 THEN '海石智能'
        WHEN 3 THEN '极星运维'
        ELSE '澜舟平台'
      END,
      50,
      'ACTIVE'
    )
    ON DUPLICATE KEY UPDATE
      password_hash = VALUES(password_hash),
      role = VALUES(role),
      nick_name = VALUES(nick_name),
      credit_balance = VALUES(credit_balance),
      status = VALUES(status);

    SET i = i + 1;
  END WHILE;

  -- More user accounts, all using user123.
  SET i = 0;
  WHILE i < 20 DO
    SET user_id_val = 300 + i;
    SET resume_id_val = 300 + i;

    INSERT INTO app_user (id, username, phone, password_hash, role, nick_name, credit_balance, status)
    VALUES (
      user_id_val,
      CONCAT('candidate_', LPAD(i + 1, 2, '0')),
      NULL,
      '$2a$10$TixSNbc1FAR4KkKUXdChW.VthBhCXC/EY4MjBwbSfmlFpX/1Xkaoa',
      'USER',
      CONCAT('候选人', LPAD(i + 1, 2, '0')),
      15 + (i MOD 12),
      'ACTIVE'
    )
    ON DUPLICATE KEY UPDATE
      password_hash = VALUES(password_hash),
      role = VALUES(role),
      nick_name = VALUES(nick_name),
      credit_balance = VALUES(credit_balance),
      status = VALUES(status);

    INSERT INTO resume (id, user_id, title, content_md, created_at, updated_at)
    VALUES (
      resume_id_val,
      user_id_val,
      CONCAT(
        CASE i MOD 5
          WHEN 0 THEN '运维开发'
          WHEN 1 THEN 'Java 后端'
          WHEN 2 THEN 'SRE'
          WHEN 3 THEN '数据平台'
          ELSE 'DevOps'
        END,
        '实习简历-', LPAD(i + 1, 2, '0')
      ),
      CONCAT(
        '## 求职方向\n',
        CASE i MOD 5
          WHEN 0 THEN '运维开发 / 云原生平台\n'
          WHEN 1 THEN 'Java 后端开发\n'
          WHEN 2 THEN 'SRE / 可观测性\n'
          WHEN 3 THEN '数据平台 / 日志分析\n'
          ELSE 'DevOps / CI/CD\n'
        END,
        '\n## 技能\n- Linux、Docker、MySQL、Redis。\n- Spring Boot 接口开发和本地 Docker 联调。\n- 了解 Prometheus、Grafana、Jenkins、Kubernetes。\n\n## 项目\n参与课程项目和个人练习项目, 完成登录、CRUD、缓存、监控或自动化脚本相关模块。'
      ),
      DATE_SUB(NOW(), INTERVAL (20 - i) DAY),
      DATE_SUB(NOW(), INTERVAL (i MOD 5) DAY)
    )
    ON DUPLICATE KEY UPDATE
      user_id = VALUES(user_id),
      title = VALUES(title),
      content_md = VALUES(content_md),
      updated_at = VALUES(updated_at);

    SET i = i + 1;
  END WHILE;

  -- 60 jobs. The first 24 belong to enterprise id 2, so the default enterprise account has pagination.
  SET i = 0;
  WHILE i < 60 DO
    SET job_id_val = 200 + i;

    IF i < 24 THEN
      SET enterprise_id_val = 2;
    ELSEIF i < 34 THEN
      SET enterprise_id_val = 6;
    ELSE
      SET enterprise_id_val = 200 + (i MOD 5);
    END IF;

    SET job_title_val = CONCAT(
      CASE i MOD 8
        WHEN 0 THEN '运维开发实习生'
        WHEN 1 THEN 'Java 后端实习生'
        WHEN 2 THEN 'SRE 实习生'
        WHEN 3 THEN 'DevOps 实习生'
        WHEN 4 THEN 'Redis 运维实习生'
        WHEN 5 THEN '可观测性平台实习生'
        WHEN 6 THEN '云原生平台实习生'
        ELSE '自动化测试开发实习生'
      END,
      '-批量', LPAD(i + 1, 2, '0')
    );

    SET jd_val = CONCAT(
      '参与',
      CASE i MOD 8
        WHEN 0 THEN '内部运维平台、自动化脚本和监控告警建设。'
        WHEN 1 THEN 'Spring Boot 业务接口、MySQL 表设计和 Redis 缓存治理。'
        WHEN 2 THEN '线上服务稳定性、告警响应、容量巡检和故障复盘。'
        WHEN 3 THEN 'Jenkins Pipeline、Docker 镜像构建、Kubernetes 发布和回滚。'
        WHEN 4 THEN 'Redis 实例巡检、慢查询分析、内存水位和淘汰策略治理。'
        WHEN 5 THEN 'Prometheus、Grafana、Loki 等可观测性平台能力建设。'
        WHEN 6 THEN 'K3s/Kubernetes 应用部署、配置管理和资源限制治理。'
        ELSE '接口自动化测试、测试数据准备和质量看板维护。'
      END,
      '要求具备 Linux 基础、良好的问题拆解能力和文档意识。'
    );

    SET stack_val = CASE i MOD 8
      WHEN 0 THEN 'Linux,Docker,Spring Boot,Redis,MySQL'
      WHEN 1 THEN 'Java,Spring Boot,MySQL,Redis,REST API'
      WHEN 2 THEN 'Linux,Prometheus,Grafana,Kubernetes,Shell'
      WHEN 3 THEN 'Jenkins,Docker,Kubernetes,Nginx,GitOps'
      WHEN 4 THEN 'Redis,Linux,Shell,Prometheus,MySQL'
      WHEN 5 THEN 'Prometheus,Grafana,Loki,Java,SQL'
      WHEN 6 THEN 'Kubernetes,K3s,Helm,ConfigMap,Secret'
      ELSE 'Java,Postman,JUnit,MySQL,CI'
    END;

    SET location_val = CASE i MOD 6
      WHEN 0 THEN '杭州'
      WHEN 1 THEN '上海'
      WHEN 2 THEN '北京'
      WHEN 3 THEN '深圳'
      WHEN 4 THEN '广州'
      ELSE '远程'
    END;

    INSERT INTO job (id, enterprise_id, title, jd_content, tech_stack, location, status, created_at, updated_at)
    VALUES (
      job_id_val,
      enterprise_id_val,
      job_title_val,
      jd_val,
      stack_val,
      location_val,
      IF(i MOD 13 = 0, 'CLOSED', 'OPEN'),
      DATE_SUB(NOW(), INTERVAL (60 - i) DAY),
      DATE_SUB(NOW(), INTERVAL (i MOD 7) DAY)
    )
    ON DUPLICATE KEY UPDATE
      enterprise_id = VALUES(enterprise_id),
      title = VALUES(title),
      jd_content = VALUES(jd_content),
      tech_stack = VALUES(tech_stack),
      location = VALUES(location),
      status = VALUES(status),
      updated_at = VALUES(updated_at);

    SET i = i + 1;
  END WHILE;

  -- 100 applications with mixed statuses for enterprise dashboards and pagination.
  SET i = 0;
  WHILE i < 100 DO
    SET app_id_val = 300 + i;
    SET job_idx = i MOD 60;
    SET job_id_val = 200 + job_idx;
    SET user_idx = ((i * 7) + (FLOOR(i / 60) * 5)) MOD 20;
    SET user_id_val = 300 + user_idx;
    SET resume_id_val = 300 + user_idx;

    SET status_val = CASE i MOD 4
      WHEN 0 THEN 'PENDING'
      WHEN 1 THEN 'VIEWED'
      WHEN 2 THEN 'ACCEPTED'
      ELSE 'REJECTED'
    END;

    SELECT enterprise_id INTO reviewed_by_val FROM job WHERE id = job_id_val;
    SET reviewed_at_val = IF(status_val = 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL (i MOD 15) DAY));

    INSERT INTO application (id, user_id, resume_id, job_id, status, remark, reviewed_by, reviewed_at, created_at, updated_at)
    VALUES (
      app_id_val,
      user_id_val,
      resume_id_val,
      job_id_val,
      status_val,
      CONCAT('批量演示投递-', LPAD(i + 1, 3, '0'), ': 希望参与该岗位相关实习。'),
      IF(status_val = 'PENDING', NULL, reviewed_by_val),
      reviewed_at_val,
      DATE_SUB(NOW(), INTERVAL (30 - (i MOD 30)) DAY),
      IF(status_val = 'PENDING', DATE_SUB(NOW(), INTERVAL (30 - (i MOD 30)) DAY), reviewed_at_val)
    )
    ON DUPLICATE KEY UPDATE
      resume_id = VALUES(resume_id),
      status = VALUES(status),
      remark = VALUES(remark),
      reviewed_by = VALUES(reviewed_by),
      reviewed_at = VALUES(reviewed_at),
      updated_at = VALUES(updated_at);

    SET i = i + 1;
  END WHILE;
END//

DELIMITER ;

CALL seed_bulk_demo();

DROP PROCEDURE IF EXISTS seed_bulk_demo;
