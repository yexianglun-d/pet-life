<template>
  <section class="page-section">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">系统配置</p>
      <h1 class="page-section__title">把系统边界、开通状态和预留模块维护清楚</h1>
      <p class="page-section__description">
        系统配置不只是放参数的地方，更是用来保证产品边界清晰、能力启停可控、后台与用户端口径一致的总入口。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">消息模板</span>
        <span class="pet-admin-chip">通知渠道</span>
        <span class="pet-admin-chip">Push 投递</span>
        <span class="pet-admin-chip">地图排查</span>
        <span class="pet-admin-chip">验证码排查</span>
        <span class="pet-admin-chip">功能开通状态</span>
        <span class="pet-admin-chip">预留模块边界</span>
      </div>
    </div>

    <div class="summary-grid">
      <article v-for="item in systemCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.highlight }}</strong>
      </article>
    </div>

    <div class="pet-admin-grid pet-admin-grid--two system-panels">
      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">服务与地图入口</h2>
        <p class="pet-admin-panel__description">
          服务商资料、城市开通、服务资源和地图坐标分开维护。地图页面只接服务端 Web 服务接口，不加载前端地图 SDK。
        </p>
        <div class="system-entry-actions">
          <el-button type="primary" @click="router.push({ name: 'serviceProviders' })">服务商管理</el-button>
          <el-button @click="router.push({ name: 'serviceMap' })">地图坐标排查</el-button>
        </div>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">通知配置入口</h2>
        <p class="pet-admin-panel__description">
          消息模板、通知渠道、验证码排查和 Push 投递记录已接入真实后台接口。短信和 Push 仍只表示底座能力，不代表真实供应商已接入。
        </p>
        <div class="system-entry-actions">
          <el-button type="primary" @click="router.push({ name: 'messageTemplates' })">消息模板管理</el-button>
          <el-button @click="router.push({ name: 'notificationChannels' })">通知渠道配置</el-button>
          <el-button @click="router.push({ name: 'smsVerifications' })">验证码排查</el-button>
          <el-button @click="router.push({ name: 'pushDeliveries' })">Push 投递排查</el-button>
        </div>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">当前预留策略</h2>
        <p class="pet-admin-panel__description">
          只有明确允许延后的模块才保持预留，不把未开发能力误包装成已完成配置。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in reservedList" :key="item">{{ item }}</li>
        </ul>
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
    description: '负责站内信、短信和 Push 模板内容、渠道配置与投递排查。',
    highlight: '已接入'
  },
  {
    title: '服务地图',
    description: '维护服务商坐标，排查高德 Web 服务配置与距离能力。',
    highlight: '排查入口'
  },
  {
    title: '验证码安全',
    description: '查看短信发送受理、频控拦截和验证码校验状态。',
    highlight: '排查入口'
  },
  {
    title: '功能开通',
    description: '控制服务中心、社区治理和后续模块的开通状态。',
    highlight: '启停控制'
  },
  {
    title: '预留模块',
    description: '明确商城与设备能力的边界，避免误入当前交付链路。',
    highlight: '边界约束'
  }
];

const reservedList = [
  '商城继续保持预留，只保留页面占位，不接入真实交易后端。',
  '设备厂商接入继续保持预留，不提前进入真实控制链路。',
  '真实短信服务、Push 推送通道和供应商 SDK 仍需后续接入。'
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
