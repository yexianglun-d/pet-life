<template>
  <section class="page-section">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">服务网络</p>
      <h1 class="page-section__title">把线下照护资源整理成可信的服务网络</h1>
      <p class="page-section__description">
        服务商后台最终要承接医院、洗护、寄养和训练等能力。这里的核心不是做电商，而是把资料、开通范围和服务边界管理清楚。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">宠物医院</span>
        <span class="pet-admin-chip">洗护美容</span>
        <span class="pet-admin-chip">寄养照看</span>
        <span class="pet-admin-chip">训练服务</span>
      </div>
    </div>

    <div class="summary-grid">
      <article v-for="item in serviceCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.highlight }}</strong>
      </article>
    </div>

    <div class="pet-admin-grid pet-admin-grid--two service-panels">
      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">后台需要维护什么</h2>
        <p class="pet-admin-panel__description">
          等真实链路展开后，这里会负责保证服务信息完整、城市开通清楚、预约资源可追踪。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in maintenanceList" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">当前设计原则</h2>
        <p class="pet-admin-panel__description">
          服务中心必须保持照护场景导向，不混入商城交易式的产品逻辑。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in principleList" :key="item">{{ item }}</li>
        </ul>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
const serviceCards = [
  {
    title: '城市开通',
    description: '控制哪些城市和哪些服务类型进入可预约状态。',
    highlight: '开通边界'
  },
  {
    title: '服务商档案',
    description: '维护门店信息、营业时间、资质标签和照护能力。',
    highlight: '基础资料'
  },
  {
    title: '预约资源',
    description: '整理接诊时间、服务时段和容量规则，为后续预约链路做准备。',
    highlight: '资源编排'
  }
];

const maintenanceList = [
  '门店基础信息、服务类型、价格区间和营业时间。',
  '可预约资源、服务时段、城市开通状态和服务半径。',
  '宠物医院、洗护、寄养、训练等不同服务类型的差异化字段。'
];

const principleList = [
  '优先服务“照护、预约、回访”流程，不做商城式导购界面。',
  '所有服务商信息都要围绕宠物生活场景组织，而不是后台字段堆积。',
  '未进入真实链路的能力保持清晰预留，不伪装成已完成页面。'
];
</script>

<style scoped>
.service-panels {
  margin-top: 24px;
}
</style>
