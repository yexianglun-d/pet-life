<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-layout__aside">
      <div class="admin-layout__brand">PetLife Admin</div>
      <el-menu :default-active="activePath" router class="admin-layout__menu">
        <el-menu-item index="/dashboard">控制台</el-menu-item>
        <el-menu-item index="/moderation">审核中心</el-menu-item>
        <el-menu-item index="/service-providers">服务商管理</el-menu-item>
        <el-menu-item index="/system-config">系统配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-layout__header">
        <div>
          <div class="admin-layout__header-title">最小可用后台</div>
          <div class="admin-layout__header-subtitle">审核 / 举报 / 服务商 / 基础配置</div>
        </div>
        <el-button text @click="handleLogout">退出</el-button>
      </el-header>
      <el-main class="admin-layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ADMIN_ACCESS_TOKEN_KEY, ADMIN_OPERATOR_NAME_KEY } from '@/shared/constants/adminSession';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const activePath = computed(() => route.path);

const handleLogout = async () => {
  window.localStorage.removeItem(ADMIN_ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(ADMIN_OPERATOR_NAME_KEY);
  await router.push({ name: 'login' });
};
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-layout__aside {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  color: #ffffff;
}

.admin-layout__brand {
  padding: 24px 20px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.admin-layout__menu {
  border-right: none;
}

.admin-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
  color: #334155;
}

.admin-layout__header-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.admin-layout__header-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.admin-layout__main {
  background: #f8fafc;
}
</style>
