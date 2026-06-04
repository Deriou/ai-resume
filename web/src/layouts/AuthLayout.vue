<template>
  <main class="auth-layout">
    <section class="brand-panel">
      <div class="brand-glow brand-glow-1" />
      <div class="brand-glow brand-glow-2" />
      <div class="brand-content">
        <div class="brand-mark">
          <span class="brand-logo">AR</span>
          AiResume
        </div>
        <h1>让简历优化、岗位匹配和 AI 成本治理<br />变成一个完整的业务闭环</h1>
        <p>
          面向求职者、企业和管理员的智能简历管理系统，覆盖登录认证、AI 调用、额度流水、投递审核和运营大盘。
        </p>
        <div class="brand-metrics">
          <div>
            <strong>智能评分</strong>
            <span>一键诊断简历短板</span>
          </div>
          <div>
            <strong>定向优化</strong>
            <span>AI 改写，贴合岗位 JD</span>
          </div>
          <div>
            <strong>岗位匹配</strong>
            <span>量化匹配度，辅助投递</span>
          </div>
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
  grid-template-columns: minmax(520px, 1fr) 540px;
  background: linear-gradient(135deg, #f5f9fe 0%, #eaf2fb 100%);
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: 72px 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(150deg, #0c4f8c 0%, #106ebe 48%, #1f8fd8 100%);
  color: #fff;
}

.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  opacity: 0.55;
  pointer-events: none;
}

.brand-glow-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  right: -100px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.55), transparent 65%);
}

.brand-glow-2 {
  width: 360px;
  height: 360px;
  bottom: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(34, 184, 239, 0.5), transparent 65%);
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.28);
}

h1 {
  margin: 32px 0 18px;
  max-width: 760px;
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

p {
  max-width: 560px;
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
}

.brand-metrics {
  margin-top: 48px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 168px));
  gap: 16px;
}

.brand-metrics div {
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.brand-metrics strong {
  display: block;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 800;
}

.brand-metrics span {
  display: block;
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
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
