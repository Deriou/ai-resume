# AiResume 环境配置说明

本文档说明 AiResume 本地和 K3s 部署所需配置项。文档只记录变量名、Secret key 和资源名称, 不记录真实密钥。

## 本地环境

默认本地 profile 为 `local`。

| 项 | 默认值 |
|---|---|
| 后端端口 | `8080` |
| 前端端口 | `5173` |
| MySQL | `127.0.0.1:3307/db_airesume` |
| Redis | `127.0.0.1:6380` |
| DeepSeek Key | `DEEPSEEK_API_KEY` |

本地启动:

```bash
docker compose up -d
./mvnw spring-boot:run
```

## 生产环境变量

后端生产环境通过 Kubernetes ConfigMap 和 Secret 注入。

### ConfigMap

建议 ConfigMap 名称:

```text
ai-resume-config
```

| 环境变量 | 用途 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | 生产环境设置为 `prod` |
| `SERVER_PORT` | 后端端口, 默认 `8080` |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 |
| `SPRING_DATA_REDIS_HOST` | Redis Service 名 |
| `SPRING_DATA_REDIS_PORT` | Redis 端口 |
| `SPRING_DATA_REDIS_TIMEOUT` | Redis 连接超时 |
| `AIRESUME_UPLOAD_BASE_DIR` | 简历附件保存目录 |
| `AIRESUME_UPLOAD_MAX_FILE_SIZE` | 单文件大小限制 |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | Spring 上传单文件限制 |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | Spring 上传请求限制 |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator 暴露端点 |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | K8s 探针支持 |

### Secret

建议 Secret 名称:

```text
ai-resume-secret
```

| Secret key | 用途 |
|---|---|
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 密码 |

Secret 示例文件:

```text
deploy/k8s/secret.example.yaml
```

不要把真实 Secret value 写入 Git。

## K3s 资源

| 类型 | 名称 |
|---|---|
| Namespace | `ai-resume` |
| ConfigMap | `ai-resume-config` |
| Secret | `ai-resume-secret` |
| imagePullSecret | `acr-secret` |
| 后端 Deployment | `ai-resume-api` |
| 后端 Service | `ai-resume-api` |
| 前端 Deployment | `ai-resume-web` |
| 前端 Service | `ai-resume-web` |
| Redis Deployment | `redis` |
| Redis Service | `redis` |
| 上传 PVC | `ai-resume-uploads-pvc` |
| Redis PVC | `ai-resume-redis-data-pvc` |
| Ingress | `ai-resume-ingress` |

## 生产连接

MySQL 复用 Cloud-Ops-Hub 现有实例:

```text
mysql.cloud-ops.svc.cluster.local:3306/db_airesume
```

Redis 部署在 AiResume namespace:

```text
redis.ai-resume.svc.cluster.local:6379
```

同 namespace 内后端可使用:

```text
SPRING_DATA_REDIS_HOST=redis
```

## 文件存储

P7 第一版使用单副本后端和 hostPath 静态 PV/PVC:

| 宿主机目录 | Pod 挂载目录 | PVC |
|---|---|---|
| `/opt/ai-resume/uploads` | `/app/uploads` | `ai-resume-uploads-pvc` |
| `/opt/ai-resume/redis-data` | `/data` | `ai-resume-redis-data-pvc` |

该方案适合单节点 K3s 演示部署。后端不要扩展到多副本, 否则本地文件会出现一致性问题。

## 域名与路由

生产域名:

```text
resume.deriou.com
```

Ingress path:

```text
/api -> ai-resume-api:8080
/   -> ai-resume-web:80
```

前后端同域部署, 前端继续请求 `/api/...`, 不需要 CORS 配置。

## 镜像

镜像仓库前缀:

```text
crpi-ekwujpeg6f954ar3.cn-wulanchabu.personal.cr.aliyuncs.com/cloud-ops-hub
```

镜像名:

```text
ai-resume-api:<tag>
ai-resume-web:<tag>
```

K8s 中拉取私有镜像需要在 `ai-resume` namespace 创建:

```text
acr-secret
```

## 可观测性

后端暴露:

```text
/actuator/prometheus
```

Prometheus 抓取目标:

```text
ai-resume-api.ai-resume.svc.cluster.local:8080/actuator/prometheus
```

LLM 指标:

```text
airesume_llm_calls_total
airesume_llm_tokens_total
airesume_llm_latency_seconds
```
