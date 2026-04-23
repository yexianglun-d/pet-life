<template>
  <section class="page-section">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">后台总览</p>
      <h1 class="page-section__title">先把陪伴体验背后的秩序整理清楚</h1>
      <p class="page-section__description">
        后台不是孤立的一套系统，它负责把社区治理、服务网络和系统边界维护好，让用户端在记录、互动和照护时更稳定。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">社区治理已接通</span>
        <span class="pet-admin-chip">服务网络待展开</span>
        <span class="pet-admin-chip">商城与设备保持预留</span>
      </div>
    </div>

    <div class="summary-grid">
      <article v-for="item in overviewCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.highlight }}</strong>
      </article>
    </div>

    <div class="pet-admin-grid pet-admin-grid--two dashboard-panels">
      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">当前重点</h2>
        <p class="pet-admin-panel__description">
          先把已经进入真实链路的后台职责做稳，再扩展服务商和系统配置能力。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in focusList" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="pet-admin-panel">
        <h2 class="pet-admin-panel__title">后台职责边界</h2>
        <p class="pet-admin-panel__description">
          后台现在主要承担运营治理与配置管理，不把商城和设备的预留能力混进当前交付范围。
        </p>
        <ul class="pet-admin-list">
          <li v-for="item in boundaryList" :key="item">{{ item }}</li>
        </ul>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
const overviewCards = [
  {
    title: '审核中心',
    description: '围绕社区帖子和举报记录，处理内容治理动作。',
    highlight: '社区治理'
  },
  {
    title: '服务商管理',
    description: '整理医院、寄养、洗护、训练等服务网络的基础信息。',
    highlight: '服务网络'
  },
  {
    title: '系统配置',
    description: '统一管理通知、开通状态和预留模块边界。',
    highlight: '系统边界'
  }
];

const focusList = [
  '把社区举报、审核处理和回写状态长期跑稳。',
  '为后续医院、洗护、寄养、训练的真实链路准备后台资料结构。',
  '把系统开通状态、消息模板和能力边界配置集中收口。'
];

const boundaryList = [
  '商城保持独立预留，不在当前后台交付范围内展开真实交易链路。',
  '设备厂商接入继续保持预留，不提前引入半成品设备控制能力。',
  '后台页面优先围绕用户端真实场景服务，不做堆砌式后台导航。'
];
</script>

<style scoped>
.dashboard-panels {
  margin-top: 24px;
}
</style>
