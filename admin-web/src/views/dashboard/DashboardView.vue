<template>
  <section class="page-section">
    <div class="dashboard-hero">
      <div>
        <p class="page-section__eyebrow">控制台</p>
        <h1 class="page-section__title">今日运营入口</h1>
      </div>
      <div class="pet-admin-chip-grid">
        <RouterLink
          v-for="action in primaryActions"
          :key="action.path"
          :to="action.path"
          class="pet-admin-chip dashboard-action"
        >
          {{ action.label }}
        </RouterLink>
      </div>
    </div>

    <div class="dashboard-module-grid">
      <article v-for="module in operationModules" :key="module.title" class="dashboard-module">
        <div>
          <p class="dashboard-module__eyebrow">{{ module.scope }}</p>
          <h2>{{ module.title }}</h2>
        </div>
        <div class="dashboard-module__links">
          <RouterLink v-for="entry in module.entries" :key="entry.path" :to="entry.path">
            {{ entry.label }}
          </RouterLink>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
const primaryActions = [
  {
    label: '处理审核任务',
    path: '/moderation-tasks'
  },
  {
    label: '查看服务商',
    path: '/service-providers'
  },
  {
    label: '查看地图排查',
    path: '/service-map'
  }
];

const operationModules = [
  {
    scope: '内容',
    title: '社区与审核',
    entries: [
      { label: '审核中心', path: '/moderation' },
      { label: '审核任务', path: '/moderation-tasks' },
      { label: '社区帖子', path: '/community-posts' },
      { label: '问答治理', path: '/community-questions' }
    ]
  },
  {
    scope: '用户',
    title: '账号与家庭',
    entries: [
      { label: '用户管理', path: '/users' },
      { label: '家庭管理', path: '/families' },
      { label: '宠物档案', path: '/pets' }
    ]
  },
  {
    scope: '记录',
    title: '宠物内容',
    entries: [
      { label: '健康记录', path: '/health-records' },
      { label: '萌宠日常', path: '/daily-logs' },
      { label: '时间轴排查', path: '/timeline-events' },
      { label: '系统提醒', path: '/system-reminders' }
    ]
  },
  {
    scope: '服务',
    title: '服务中心',
    entries: [
      { label: '服务商管理', path: '/service-providers' },
      { label: '地图排查', path: '/service-map' }
    ]
  },
  {
    scope: '通知',
    title: '消息与 Push',
    entries: [
      { label: '提醒模板', path: '/reminder-templates' },
      { label: '消息模板', path: '/message-templates' },
      { label: '通知渠道', path: '/notification-channels' },
      { label: 'Push 投递', path: '/push-deliveries' }
    ]
  },
  {
    scope: '系统',
    title: '配置与排查',
    entries: [
      { label: '验证码排查', path: '/sms-verifications' },
      { label: '系统配置', path: '/system-config' }
    ]
  }
];
</script>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
  padding: 28px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 30px;
  background: linear-gradient(135deg, #ffe9da 0%, #fffaf5 100%);
  box-shadow: var(--pet-admin-shadow);
}

.dashboard-action {
  color: var(--pet-admin-title);
  text-decoration: none;
}

.dashboard-module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.dashboard-module {
  display: flex;
  min-height: 178px;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 24px;
  background: var(--pet-admin-surface);
  box-shadow: var(--pet-admin-shadow);
}

.dashboard-module__eyebrow {
  margin: 0 0 8px;
  color: var(--pet-admin-primary-deep);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.dashboard-module h2 {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 22px;
  line-height: 1.3;
}

.dashboard-module__links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.dashboard-module__links a {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 7px 12px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 999px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-body);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.dashboard-module__links a:hover,
.dashboard-action:hover {
  color: var(--pet-admin-primary-deep);
}

@media (max-width: 900px) {
  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
