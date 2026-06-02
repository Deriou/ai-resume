<template>
  <main class="auth-layout">
    <section class="brand-panel">
      <div class="brand-mark">AiResume</div>
      <h1>让简历优化、岗位匹配和 AI 成本治理变成一个完整业务闭环</h1>
      <p>
        面向求职者、企业和管理员的智能简历管理系统，覆盖登录认证、AI 调用、额度流水、投递审核和运营大盘。
      </p>
      <div class="brand-metrics">
        <div>
          <strong>3</strong>
          <span>角色工作台</span>
        </div>
        <div>
          <strong>AI</strong>
          <span>评分 / 优化 / 匹配</span>
        </div>
        <div>
          <strong>Redis</strong>
          <span>验证码 / 登录态 / 热点</span>
        </div>
      </div>
    </section>
    <section class="form-panel">
      <router-view />
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(520px, 1fr) 520px;
  background:
    radial-gradient(circle at 15% 15%, rgba(37, 99, 235, 0.16), transparent 28%),
    linear-gradient(135deg, #f7fbff 0%, #eef5ff 100%);
}

.brand-panel {
  padding: 72px 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-mark {
  width: max-content;
  padding: 10px 14px;
  border-radius: 8px;
  background: #0f172a;
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.3px;
}

h1 {
  margin: 28px 0 18px;
  max-width: 720px;
  font-size: 44px;
  line-height: 1.15;
  color: #102033;
}

p {
  max-width: 620px;
  margin: 0;
  font-size: 17px;
  line-height: 1.8;
  color: #53637a;
}

.brand-metrics {
  margin-top: 42px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 160px));
  gap: 16px;
}

.brand-metrics div {
  padding: 18px;
  border: 1px solid rgba(37, 99, 235, 0.14);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--app-shadow-soft);
}

.brand-metrics strong {
  display: block;
  font-size: 24px;
  color: var(--app-primary);
}

.brand-metrics span {
  display: block;
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: rgba(255, 255, 255, 0.72);
  border-left: 1px solid rgba(37, 99, 235, 0.12);
}

@media (max-width: 1100px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }
}
</style>
