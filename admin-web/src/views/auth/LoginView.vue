<template>
  <div class="login-page">
    <section class="login-shell">
      <article class="login-intro">
        <p class="login-intro__eyebrow">PetLife Admin</p>
        <h1 class="login-intro__title">陪伴体验背后的运营工作台</h1>
        <p class="login-intro__description">
          后台负责把社区治理、服务配置和系统边界整理清楚，让用户端看到的每一步都更稳定、更可信。
        </p>
        <div class="login-intro__chips">
          <span class="login-intro__chip">社区审核</span>
          <span class="login-intro__chip">举报处理</span>
          <span class="login-intro__chip">服务商维护</span>
          <span class="login-intro__chip">系统配置</span>
        </div>
        <div class="login-intro__note">
          当前重点依然是围绕宠物生活场景，做清晰、稳定、可持续的运营支撑，而不是堆砌后台功能。
        </div>
      </article>

      <section class="login-panel">
        <header class="login-panel__header">
          <p class="login-panel__eyebrow">欢迎回来</p>
          <h2 class="login-panel__title">进入后台</h2>
          <p class="login-panel__description">
            登录后可查看审核中心、服务商管理和系统配置页。
          </p>
        </header>

        <el-form label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="账号">
            <el-input v-model="account" placeholder="请输入后台账号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="password" type="password" show-password placeholder="请输入后台密码" />
          </el-form-item>
          <el-button type="primary" class="login-panel__submit" :loading="isSubmitting" @click="handleSubmit">
            {{ isSubmitting ? '正在进入后台...' : '进入后台' }}
          </el-button>
        </el-form>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { loginAdmin } from '@/shared/api/adminApi';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const account = ref('admin');
const password = ref('petlife123');
const isSubmitting = ref(false);

const handleSubmit = async () => {
  if (!account.value.trim() || !password.value.trim()) {
    ElMessage.warning('请输入账号和密码');
    return;
  }

  isSubmitting.value = true;
  try {
    await loginAdmin(account.value.trim(), password.value);
    ElMessage.success('已进入后台');
    await router.push({ name: 'dashboard' });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '后台登录失败');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(255, 233, 218, 0.92), transparent 26%),
    radial-gradient(circle at bottom right, rgba(232, 243, 231, 0.86), transparent 22%),
    linear-gradient(180deg, #fffaf5 0%, #fff6ef 100%);
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 420px);
  gap: 22px;
  width: min(1080px, 100%);
  align-items: stretch;
}

.login-intro,
.login-panel {
  border: 1px solid var(--pet-admin-line);
  border-radius: 32px;
  background: rgba(255, 253, 251, 0.92);
  box-shadow: var(--pet-admin-shadow);
}

.login-intro {
  padding: 34px;
  background: linear-gradient(135deg, #ffe9da 0%, #fffaf5 100%);
}

.login-intro__eyebrow,
.login-panel__eyebrow {
  margin: 0 0 10px;
  color: var(--pet-admin-primary-deep);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.login-intro__title,
.login-panel__title {
  margin: 0;
  color: var(--pet-admin-title);
}

.login-intro__title {
  max-width: 480px;
  font-size: 38px;
  line-height: 1.15;
}

.login-intro__description,
.login-panel__description {
  color: var(--pet-admin-body);
  line-height: 1.8;
}

.login-intro__description {
  max-width: 520px;
  margin: 14px 0 0;
}

.login-intro__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.login-intro__chip {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.login-intro__note {
  margin-top: 26px;
  padding: 18px;
  border: 1px solid rgba(201, 103, 79, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--pet-admin-body);
  line-height: 1.8;
}

.login-panel {
  padding: 30px;
}

.login-panel__header {
  margin-bottom: 28px;
}

.login-panel__description {
  margin: 12px 0 0;
}

.login-panel__submit {
  width: 100%;
  height: 50px;
  margin-top: 14px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #e68c72 0%, #d7745a 100%);
  box-shadow: 0 14px 26px rgba(217, 118, 105, 0.22);
}

:deep(.el-form-item__label) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

:deep(.el-input__wrapper) {
  border-radius: 18px;
  background: var(--pet-admin-surface-soft);
  box-shadow: 0 0 0 1px var(--pet-admin-line) inset;
}

:deep(.el-input__inner) {
  color: var(--pet-admin-title);
}

@media (max-width: 900px) {
  .login-page {
    padding: 18px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-intro {
    padding: 26px;
  }

  .login-intro__title {
    font-size: 30px;
  }
}
</style>
