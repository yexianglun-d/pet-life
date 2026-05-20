<template>
  <section class="page-section">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">系统配置</p>
      <h1 class="page-section__title">系统配置</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">消息模板</span>
        <span class="pet-admin-chip">通知渠道</span>
        <span class="pet-admin-chip">Push 投递</span>
        <span class="pet-admin-chip">地图排查</span>
        <span class="pet-admin-chip">验证码排查</span>
        <span class="pet-admin-chip">功能开通状态</span>
      </div>
    </div>

    <div class="summary-grid">
      <article v-for="item in systemCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.highlight }}</strong>
      </article>
    </div>

    <div class="pet-admin-grid pet-admin-grid--two system-panels">
      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">服务与地图入口</h2>
        <div class="system-entry-actions">
          <el-button type="primary" @click="router.push({ name: 'serviceProviders' })">服务商管理</el-button>
          <el-button @click="router.push({ name: 'serviceMap' })">地图坐标排查</el-button>
        </div>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">通知配置入口</h2>
        <div class="system-entry-actions">
          <el-button type="primary" @click="router.push({ name: 'messageTemplates' })">消息模板管理</el-button>
          <el-button @click="router.push({ name: 'notificationChannels' })">通知渠道配置</el-button>
          <el-button @click="router.push({ name: 'smsVerifications' })">验证码排查</el-button>
          <el-button @click="router.push({ name: 'pushDeliveries' })">Push 投递排查</el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const systemCards = [
  {
    title: '通知与消息',
    highlight: '已接入'
  },
  {
    title: '服务地图',
    highlight: '排查入口'
  },
  {
    title: '验证码安全',
    highlight: '排查入口'
  },
  {
    title: '功能开通',
    highlight: '启停控制'
  }
];
</script>

<style scoped>
.system-panels {
  margin-top: 24px;
}

.system-entry-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.el-button) {
  border-radius: 14px;
}
</style>
