<template>
  <div class="login-page">
    <section class="login-panel">
      <header class="login-panel__header">
        <p class="login-panel__eyebrow">PetLife Admin</p>
        <h1 class="login-panel__title">后台登录</h1>
        <p class="login-panel__description">
          当前阶段只开放审核、举报处理、服务商维护和系统配置等最小可用后台能力。
        </p>
      </header>

      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="账号">
          <el-input v-model="account" placeholder="请输入后台账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入后台密码" />
        </el-form-item>
        <el-button type="primary" class="login-panel__submit" @click="handleSubmit">
          进入后台
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const ADMIN_ACCESS_TOKEN_KEY = 'petlife_admin_access_token';

const router = useRouter();
const account = ref('admin');
const password = ref('petlife123');

const handleSubmit = async () => {
  if (!account.value.trim() || !password.value.trim()) {
    ElMessage.warning('请输入账号和密码');
    return;
  }

  window.localStorage.setItem(ADMIN_ACCESS_TOKEN_KEY, 'bootstrap-admin-token');
  ElMessage.success('已进入后台骨架');
  await router.push({ name: 'dashboard' });
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f8fafc;
}

.login-panel {
  width: min(420px, 100%);
  padding: 32px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.06);
}

.login-panel__header {
  margin-bottom: 28px;
}

.login-panel__eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.login-panel__title {
  margin: 0 0 10px;
  font-size: 30px;
  line-height: 1.15;
  color: #0f172a;
}

.login-panel__description {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.login-panel__submit {
  width: 100%;
  margin-top: 12px;
}
</style>
