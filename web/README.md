# AiResume Web Frontend

Vue 3 + Vite + TypeScript + Element Plus 前端，对应 P5 业务演示。

## 启动

```bash
# 1. 启动 MySQL + Redis（项目根目录）
docker compose up -d

# 2. 启动后端（项目根目录）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. 启动前端
cd web
npm install
npm run dev
```

访问：http://localhost:5173

## 种子账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| user | user123 | 求职者 |
| enterprise | enterprise123 | 企业 |
| admin | admin123 | 管理员 |

## 构建

```bash
npm run build
```

产物输出到 `web/dist/`。

## 目录

```text
src/api/        Axios 接口封装
src/stores/     Pinia 状态
src/router/     路由与守卫
src/views/      页面（auth / user / enterprise / admin）
src/components/ 通用与 AI 结果组件
src/types/      TypeScript 类型
```

本地开发通过 Vite proxy 将 `/api` 转发到 `http://localhost:8080`。
