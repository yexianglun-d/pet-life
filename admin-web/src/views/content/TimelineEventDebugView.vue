<template>
  <section class="page-section timeline-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">成长时间轴</p>
      <h1 class="page-section__title">排查时间轴事件和源记录派生状态</h1>
      <p class="page-section__description">
        时间轴是健康、日常和服务预约的读模型。这个页面用于确认事件是否存在、来源是否还有效、可见范围是否符合预期。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">事件 {{ records.length }} 条</span>
        <span class="pet-admin-chip">源记录有效 {{ activeSourceCount }} 条</span>
        <span class="pet-admin-chip">源异常 {{ brokenSourceCount }} 条</span>
        <span class="pet-admin-chip">公开 {{ publicCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid timeline-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel timeline-section">
      <div class="timeline-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">时间轴事件</h2>
          <p class="pet-admin-panel__description">
            按事件类型、源类型、宠物 ID 或源记录 ID 查询。源状态异常时，需要回到对应服务端派生链路排查。
          </p>
        </div>
        <div class="timeline-toolbar__actions">
          <el-select v-model="filters.eventType" size="small" class="timeline-filter" placeholder="事件类型">
            <el-option label="全部事件" value="all" />
            <el-option label="健康" value="health" />
            <el-option label="日常" value="daily_log" />
            <el-option label="服务" value="service" />
          </el-select>
          <el-select v-model="filters.sourceType" size="small" class="timeline-filter" placeholder="源类型">
            <el-option label="全部来源" value="all" />
            <el-option label="健康记录" value="health_record" />
            <el-option label="萌宠日常" value="daily_log" />
            <el-option label="服务预约" value="service_appointment" />
          </el-select>
          <el-input v-model="filters.petId" size="small" class="timeline-filter" placeholder="宠物 ID" clearable />
          <el-input v-model="filters.sourceId" size="small" class="timeline-filter" placeholder="源记录 ID" clearable />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="timeline-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="timeline_event.event_id"
        empty-text="暂无时间轴事件"
        class="timeline-table"
      >
        <el-table-column label="事件" min-width="320">
          <template #default="{ row }">
            <div class="timeline-cell">
              <div class="timeline-cell__title">
                <span>{{ row.timeline_event.title }}</span>
                <el-tag size="small" :type="eventTypeTagType(row.timeline_event.event_type)">
                  {{ eventTypeLabel(row.timeline_event.event_type) }}
                </el-tag>
              </div>
              <div class="timeline-cell__meta">
                #{{ row.timeline_event.event_id }} · {{ formatDateTime(row.timeline_event.event_time) }}
              </div>
              <div class="timeline-cell__detail">{{ row.timeline_event.summary || '暂无摘要' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="来源" min-width="230">
          <template #default="{ row }">
            <div class="timeline-cell">
              <div class="timeline-cell__title">
                <span>{{ sourceTypeLabel(row.timeline_event.source_type) }}</span>
                <el-tag size="small" :type="sourceStatusTagType(row.source_status)">
                  {{ sourceStatusLabel(row.source_status) }}
                </el-tag>
              </div>
              <div class="timeline-cell__meta">源记录 ID：{{ row.timeline_event.source_id }}</div>
              <div class="timeline-cell__meta">可见范围：{{ visibilityLabel(row.timeline_event.visibility) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="宠物归属" min-width="230">
          <template #default="{ row }">
            <div class="timeline-cell">
              <div class="timeline-cell__title">{{ row.pet.pet_name }}</div>
              <div class="timeline-cell__meta">
                {{ petTypeLabel(row.pet.pet_type) }} · {{ row.pet.family_name || '暂无家庭' }}
              </div>
              <div class="timeline-cell__meta">
                主人：{{ row.pet.owner_nickname || '未知主人' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.timeline_event.created_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="时间轴事件详情" size="520px">
      <div v-loading="detailLoading" class="timeline-detail">
        <template v-if="activeRecord">
          <section class="timeline-detail__section">
            <h3>{{ activeRecord.timeline_event.title }}</h3>
            <div class="timeline-detail__tags">
              <el-tag :type="eventTypeTagType(activeRecord.timeline_event.event_type)">
                {{ eventTypeLabel(activeRecord.timeline_event.event_type) }}
              </el-tag>
              <el-tag :type="sourceStatusTagType(activeRecord.source_status)">
                {{ sourceStatusLabel(activeRecord.source_status) }}
              </el-tag>
            </div>
            <dl class="timeline-detail-list">
              <div>
                <dt>事件时间</dt>
                <dd>{{ formatDateTime(activeRecord.timeline_event.event_time) }}</dd>
              </div>
              <div>
                <dt>来源</dt>
                <dd>
                  {{ sourceTypeLabel(activeRecord.timeline_event.source_type) }}
                  #{{ activeRecord.timeline_event.source_id }}
                </dd>
              </div>
              <div>
                <dt>可见范围</dt>
                <dd>{{ visibilityLabel(activeRecord.timeline_event.visibility) }}</dd>
              </div>
              <div>
                <dt>摘要</dt>
                <dd>{{ activeRecord.timeline_event.summary || '-' }}</dd>
              </div>
              <div>
                <dt>封面</dt>
                <dd>{{ activeRecord.timeline_event.cover_url || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="timeline-detail__section">
            <h3>宠物归属</h3>
            <dl class="timeline-detail-list">
              <div>
                <dt>宠物</dt>
                <dd>{{ activeRecord.pet.pet_name }} · {{ petTypeLabel(activeRecord.pet.pet_type) }}</dd>
              </div>
              <div>
                <dt>家庭</dt>
                <dd>{{ activeRecord.pet.family_name || '-' }}</dd>
              </div>
              <div>
                <dt>主人</dt>
                <dd>{{ activeRecord.pet.owner_nickname || '-' }} · {{ activeRecord.pet.owner_mobile || '-' }}</dd>
              </div>
            </dl>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import {
  getAdminTimelineEvent,
  listAdminTimelineEvents,
  type AdminTimelineEventSnapshot,
  type DailyLogVisibility,
  type TimelineEventType,
  type TimelineEventTypeFilter,
  type TimelineSourceStatus,
  type TimelineSourceType,
  type TimelineSourceTypeFilter
} from '@/shared/api/adminContentApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const records = ref<AdminTimelineEventSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminTimelineEventSnapshot | null>(null);

const filters = reactive<{
  eventType: TimelineEventTypeFilter;
  sourceType: TimelineSourceTypeFilter;
  petId: string;
  sourceId: string;
}>({
  eventType: 'all',
  sourceType: 'all',
  petId: '',
  sourceId: ''
});

const activeSourceCount = computed(
  () => records.value.filter((record) => record.source_status === 'active').length
);
const brokenSourceCount = computed(
  () => records.value.filter((record) => record.source_status !== 'active').length
);
const publicCount = computed(
  () => records.value.filter((record) => record.timeline_event.visibility === 'public').length
);
const serviceEventCount = computed(
  () => records.value.filter((record) => record.timeline_event.event_type === 'service').length
);

const summaryCards = computed(() => [
  {
    title: '事件总数',
    description: '当前筛选条件下返回的时间轴事件。',
    value: `${records.value.length} 条`
  },
  {
    title: '源记录有效',
    description: '源记录仍存在且可追溯。',
    value: `${activeSourceCount.value} 条`
  },
  {
    title: '源异常',
    description: '源记录缺失、删除或暂不支持核验。',
    value: `${brokenSourceCount.value} 条`
  },
  {
    title: '服务事件',
    description: '由服务预约派生的时间轴事件。',
    value: `${serviceEventCount.value} 条`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminTimelineEvents({
      eventType: filters.eventType,
      sourceType: filters.sourceType,
      petId: filters.petId,
      sourceId: filters.sourceId
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '时间轴事件加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminTimelineEventSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminTimelineEvent(record.timeline_event.event_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '时间轴事件详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function eventTypeLabel(eventType: TimelineEventType) {
  const labelMap: Record<TimelineEventType, string> = {
    health: '健康',
    daily_log: '日常',
    service: '服务'
  };
  return labelMap[eventType];
}

function eventTypeTagType(eventType: TimelineEventType) {
  if (eventType === 'health') {
    return 'success';
  }
  if (eventType === 'service') {
    return 'warning';
  }
  return 'info';
}

function sourceTypeLabel(sourceType: TimelineSourceType) {
  const labelMap: Record<TimelineSourceType, string> = {
    health_record: '健康记录',
    daily_log: '萌宠日常',
    service_appointment: '服务预约'
  };
  return labelMap[sourceType];
}

function sourceStatusLabel(sourceStatus: TimelineSourceStatus) {
  const labelMap: Record<TimelineSourceStatus, string> = {
    active: '有效',
    deleted: '已删除',
    missing: '缺失',
    unsupported: '暂不支持核验'
  };
  return labelMap[sourceStatus];
}

function sourceStatusTagType(sourceStatus: TimelineSourceStatus) {
  if (sourceStatus === 'active') {
    return 'success';
  }
  if (sourceStatus === 'unsupported') {
    return 'info';
  }
  return 'danger';
}

function visibilityLabel(visibility: DailyLogVisibility) {
  const labelMap: Record<DailyLogVisibility, string> = {
    private: '仅自己',
    family: '家庭可见',
    public: '公开'
  };
  return labelMap[visibility];
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗'
  };
  return labelMap[petType] ?? petType;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.timeline-summary,
.timeline-section {
  margin-top: 24px;
}

.timeline-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.timeline-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.timeline-filter {
  width: 138px;
}

.timeline-error {
  margin-bottom: 16px;
}

.timeline-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.timeline-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.timeline-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.timeline-cell__meta,
.timeline-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.timeline-cell__detail {
  color: var(--pet-admin-body);
}

.timeline-detail {
  min-height: 320px;
}

.timeline-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.timeline-detail__section:first-child {
  padding-top: 0;
}

.timeline-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.timeline-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.timeline-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.timeline-detail-list div {
  display: grid;
  gap: 5px;
}

.timeline-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.timeline-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

:deep(.el-table) {
  --el-table-border-color: var(--pet-admin-line);
  --el-table-header-bg-color: #fff8f2;
  --el-table-row-hover-bg-color: #fff6ee;
  --el-table-text-color: var(--pet-admin-body);
  --el-table-header-text-color: var(--pet-admin-title);
  border-radius: 20px;
}

:deep(.el-button),
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-drawer) {
  border-radius: 14px;
}

:deep(.el-drawer__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .timeline-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .timeline-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .timeline-filter {
    width: 100%;
  }

  .timeline-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
