<template>
  <section class="page-section">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">系统配置</p>
      <h1 class="page-section__title">把系统边界、开通状态和预留模块维护清楚</h1>
      <p class="page-section__description">
        系统配置不只是放参数的地方，更是用来保证产品边界清晰、能力启停可控、后台与用户端口径一致的总开关。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">通知模板</span>
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
        <h2 class="pet-admin-panel__title">配置重点</h2>
        <p class="pet-admin-panel__description">
          后续真实接入时，这里会承接系统级别的开通、关闭和边界控制。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in configList" :key="item">{{ item }}</li>
        </ul>
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
const systemCards = [
  {
    title: '通知与消息',
    description: '负责短信、站内通知和模板内容的统一管理。',
    highlight: '消息策略'
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

const configList = [
  '消息模板、通知渠道和提醒类文案的统一配置。',
  '服务中心、社区、后台治理能力的系统级开关。',
  '影响用户端展示和后台入口的基础配置收口。'
];

const reservedList = [
  '商城继续保持预留，只保留页面占位，不接入真实交易后端。',
  '设备厂商接入继续保持预留，不提前进入真实控制链路。',
  '任何未完成能力都必须明确标注边界，不伪装成已上线模块。'
];
</script>

<style scoped>
.system-panels {
  margin-top: 24px;
}
</style>
