<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-layout__aside">
      <div class="admin-layout__brand">
        <div class="admin-layout__brand-title">宠物生活管家</div>
        <div class="admin-layout__brand-subtitle">运营与治理后台</div>
      </div>
      <el-menu :default-active="activePath" router class="admin-layout__menu">
        <el-menu-item index="/dashboard">控制台</el-menu-item>
        <el-menu-item index="/moderation">审核中心</el-menu-item>
        <el-menu-item index="/moderation-tasks">审核任务</el-menu-item>
        <el-menu-item index="/community-posts">社区帖子</el-menu-item>
        <el-menu-item index="/community-questions">问答治理</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/families">家庭管理</el-menu-item>
        <el-menu-item index="/pets">宠物档案</el-menu-item>
        <el-menu-item index="/health-records">健康记录</el-menu-item>
        <el-menu-item index="/daily-logs">萌宠日常</el-menu-item>
        <el-menu-item index="/timeline-events">时间轴排查</el-menu-item>
        <el-menu-item index="/system-reminders">系统提醒</el-menu-item>
        <el-menu-item index="/reminder-templates">提醒模板</el-menu-item>
        <el-menu-item index="/sms-verifications">验证码排查</el-menu-item>
        <el-menu-item index="/message-templates">消息模板</el-menu-item>
        <el-menu-item index="/notification-channels">通知渠道</el-menu-item>
        <el-menu-item index="/push-deliveries">Push 投递</el-menu-item>
        <el-menu-item index="/service-providers">服务商管理</el-menu-item>
        <el-menu-item index="/service-map">地图排查</el-menu-item>
        <el-menu-item index="/system-config">系统配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-layout__header">
        <div>
          <div class="admin-layout__header-title">{{ activeMenuTitle }}</div>
        </div>
        <div class="admin-layout__header-actions">
          <div class="admin-layout__operator">{{ operatorName }}</div>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { clearAdminSession, logoutAdmin } from '@/shared/api/adminApi';
import { ADMIN_OPERATOR_NAME_KEY } from '@/shared/constants/adminSession';
import { ElMessage } from 'element-plus';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const menuTitleMap = new Map([
  ['/dashboard', '控制台'],
  ['/moderation', '审核中心'],
  ['/moderation-tasks', '审核任务'],
  ['/community-posts', '社区帖子'],
  ['/community-questions', '问答治理'],
  ['/users', '用户管理'],
  ['/families', '家庭管理'],
  ['/pets', '宠物档案'],
  ['/health-records', '健康记录'],
  ['/daily-logs', '萌宠日常'],
  ['/timeline-events', '时间轴排查'],
  ['/system-reminders', '系统提醒'],
  ['/reminder-templates', '提醒模板'],
  ['/sms-verifications', '验证码排查'],
  ['/message-templates', '消息模板'],
  ['/notification-channels', '通知渠道'],
  ['/push-deliveries', 'Push 投递'],
  ['/service-providers', '服务商管理'],
  ['/service-map', '地图排查'],
  ['/system-config', '系统配置']
]);

const activePath = computed(() => route.path);
const activeMenuTitle = computed(() => menuTitleMap.get(route.path) ?? '宠物生活管家后台');
const operatorName = computed(
  () => window.localStorage.getItem(ADMIN_OPERATOR_NAME_KEY) ?? '当前管理员'
);

const handleLogout = async () => {
  try {
    await logoutAdmin();
  } catch (error) {
    clearAdminSession();
    ElMessage.warning(error instanceof Error ? error.message : '后台退出接口异常，已清理本地登录态');
  }
  await router.push({ name: 'login' });
};
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-layout__aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--pet-admin-line);
  background: linear-gradient(180deg, #fff7f0 0%, #fffdf9 100%);
  color: var(--pet-admin-title);
}

.admin-layout__brand {
  padding: 28px 22px 18px;
}

.admin-layout__brand-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--pet-admin-title);
}

.admin-layout__brand-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--pet-admin-muted);
  letter-spacing: 0.04em;
}

.admin-layout__menu {
  flex: 1;
  border-right: none;
  background: transparent;
  overflow-y: auto;
}

.admin-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 84px;
  border-bottom: 1px solid var(--pet-admin-line);
  background: rgba(255, 253, 251, 0.88);
  backdrop-filter: blur(12px);
  color: var(--pet-admin-title);
}

.admin-layout__header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--pet-admin-title);
}

.admin-layout__header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-layout__operator {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.admin-layout__main {
  background: transparent;
}

:deep(.el-menu) {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--pet-admin-body);
  --el-menu-hover-bg-color: rgba(230, 140, 114, 0.12);
  --el-menu-active-color: var(--pet-admin-primary-deep);
  --el-menu-item-height: 48px;
  --el-menu-item-font-size: 14px;
  padding: 0 14px 18px;
}

:deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 16px;
}

:deep(.el-menu-item.is-active) {
  background: rgba(230, 140, 114, 0.14);
  font-weight: 700;
}

:deep(.el-header) {
  padding: 0 28px;
}

@media (max-width: 900px) {
  .admin-layout__aside {
    width: 200px !important;
  }

  :deep(.el-header) {
    padding: 0 18px;
  }
}
</style>
