<template>
  <section class="page-section health-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">健康档案</p>
      <h1 class="page-section__title">健康记录审查</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">记录 {{ records.length }} 条</span>
        <span class="pet-admin-chip">带附件 {{ attachmentRecordCount }} 条</span>
        <span class="pet-admin-chip">有后续提醒 {{ nextReminderCount }} 条</span>
        <span class="pet-admin-chip">异常观察 {{ observationCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid health-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel health-section">
      <div class="health-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">健康记录队列</h2>
        </div>
        <div class="health-toolbar__actions">
          <el-select v-model="filters.recordType" size="small" class="health-filter" placeholder="记录类型">
            <el-option label="全部类型" value="all" />
            <el-option
              v-for="option in healthRecordTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input v-model="filters.petId" size="small" class="health-filter" placeholder="宠物 ID" clearable />
          <el-input v-model="filters.operatorUserId" size="small" class="health-filter" placeholder="操作者 ID" clearable />
          <el-input v-model="filters.keyword" size="small" class="health-keyword" placeholder="标题 / 备注 / 宠物 / 主人" clearable />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="health-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="health_record.health_record_id"
        empty-text="暂无健康记录"
        class="health-table"
      >
        <el-table-column label="健康记录" min-width="280">
          <template #default="{ row }">
            <div class="health-cell">
              <div class="health-cell__title">
                <span>{{ row.health_record.title }}</span>
                <el-tag size="small" :type="recordTypeTagType(row.health_record.record_type)">
                  {{ recordTypeLabel(row.health_record.record_type) }}
                </el-tag>
              </div>
              <div class="health-cell__meta">
                {{ formatDateTime(row.health_record.occurred_at) }}
              </div>
              <div class="health-cell__detail">
                {{ row.health_record.result_summary || row.health_record.notes || '暂无结果摘要' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="宠物归属" min-width="240">
          <template #default="{ row }">
            <div class="health-cell">
              <div class="health-cell__title">{{ row.pet.pet_name }}</div>
              <div class="health-cell__meta">
                {{ petTypeLabel(row.pet.pet_type) }} · {{ row.pet.family_name || '暂无家庭' }}
              </div>
              <div class="health-cell__meta">
                主人：{{ row.pet.owner_nickname || '未知主人' }}
                <span v-if="row.pet.owner_mobile">· {{ row.pet.owner_mobile }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作者" min-width="180">
          <template #default="{ row }">
            <div class="health-cell">
              <div class="health-cell__title">{{ row.operator.nickname || '未知用户' }}</div>
              <div class="health-cell__meta">ID：{{ row.operator.user_id }}</div>
              <div v-if="row.operator.mobile" class="health-cell__meta">{{ row.operator.mobile }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="附件" width="130">
          <template #default="{ row }">
            <div class="health-count-stack">
              <strong>{{ row.health_record.attachment_assets.length }}</strong>
              <span>个附件</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="后续提醒" min-width="180">
          <template #default="{ row }">
            <div class="health-cell">
              <el-tag v-if="row.health_record.next_reminder_id" size="small" type="success">
                {{ reminderStatusLabel(row.health_record.next_reminder_status) }}
              </el-tag>
              <span v-else class="health-cell__meta">暂无后续提醒</span>
              <div v-if="row.health_record.next_reminder_at" class="health-cell__meta">
                {{ formatDateTime(row.health_record.next_reminder_at) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="健康记录详情" size="520px">
      <div v-loading="detailLoading" class="health-detail">
        <template v-if="activeRecord">
          <section class="health-detail__section">
            <h3>{{ activeRecord.health_record.title }}</h3>
            <div class="health-detail__tags">
              <el-tag :type="recordTypeTagType(activeRecord.health_record.record_type)">
                {{ recordTypeLabel(activeRecord.health_record.record_type) }}
              </el-tag>
              <el-tag v-if="activeRecord.health_record.severity_level" type="warning">
                {{ severityLabel(activeRecord.health_record.severity_level) }}
              </el-tag>
            </div>
            <dl class="health-detail-list">
              <div>
                <dt>发生时间</dt>
                <dd>{{ formatDateTime(activeRecord.health_record.occurred_at) }}</dd>
              </div>
              <div v-if="activeRecord.health_record.value || activeRecord.health_record.unit">
                <dt>记录值</dt>
                <dd>{{ valueLabel(activeRecord.health_record.value, activeRecord.health_record.unit) }}</dd>
              </div>
              <div>
                <dt>医院 / 医生</dt>
                <dd>{{ activeRecord.health_record.hospital_name || '-' }} / {{ activeRecord.health_record.doctor_name || '-' }}</dd>
              </div>
              <div>
                <dt>结果摘要</dt>
                <dd>{{ activeRecord.health_record.result_summary || '-' }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ activeRecord.health_record.notes || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="health-detail__section">
            <h3>宠物与用户</h3>
            <dl class="health-detail-list">
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
              <div>
                <dt>操作者</dt>
                <dd>{{ activeRecord.operator.nickname || '-' }} · {{ activeRecord.operator.mobile || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="health-detail__section">
            <h3>附件</h3>
            <div v-if="activeRecord.health_record.attachment_assets.length === 0" class="health-empty-inline">
              暂无附件
            </div>
            <div v-else class="health-asset-list">
              <article
                v-for="asset in activeRecord.health_record.attachment_assets"
                :key="asset.asset_id"
                class="health-asset"
              >
                <div class="health-asset__title">
                  <span>{{ asset.file_name }}</span>
                  <el-tag size="small" type="info">{{ mediaTypeLabel(asset.media_type) }}</el-tag>
                </div>
                <div class="health-cell__meta">
                  {{ asset.content_type }} · {{ formatFileSize(asset.file_size) }}
                </div>
                <div class="health-cell__meta">
                  上传：{{ asset.completed_at ? formatDateTime(asset.completed_at) : '未完成' }}
                </div>
                <div class="health-cell__meta">
                  审核：{{ asset.review_status }} · 状态：{{ asset.upload_status }}
                </div>
              </article>
            </div>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import {
  getAdminHealthRecord,
  listAdminHealthRecords,
  type AdminHealthRecordSnapshot,
  type HealthRecordType,
  type HealthRecordTypeFilter,
  type MediaType
} from '@/shared/api/adminContentApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const healthRecordTypeOptions: Array<{ label: string; value: HealthRecordType }> = [
  { label: '疫苗', value: 'vaccine' },
  { label: '驱虫', value: 'deworming' },
  { label: '体检', value: 'examination' },
  { label: '用药', value: 'medication' },
  { label: '体重', value: 'weight' },
  { label: '异常观察', value: 'observation' }
];

const records = ref<AdminHealthRecordSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminHealthRecordSnapshot | null>(null);

const filters = reactive<{
  recordType: HealthRecordTypeFilter;
  petId: string;
  operatorUserId: string;
  keyword: string;
}>({
  recordType: 'all',
  petId: '',
  operatorUserId: '',
  keyword: ''
});

const attachmentRecordCount = computed(
  () => records.value.filter((record) => record.health_record.attachment_assets.length > 0).length
);
const nextReminderCount = computed(
  () => records.value.filter((record) => Boolean(record.health_record.next_reminder_id)).length
);
const observationCount = computed(
  () => records.value.filter((record) => record.health_record.record_type === 'observation').length
);
const abnormalCount = computed(
  () => records.value.filter((record) => Boolean(record.health_record.severity_level)).length
);

const summaryCards = computed(() => [
  {
    title: '记录总数',
    description: '当前筛选条件下返回的健康记录。',
    value: `${records.value.length} 条`
  },
  {
    title: '附件记录',
    description: '含图片或 PDF 等附件的记录。',
    value: `${attachmentRecordCount.value} 条`
  },
  {
    title: '自动提醒',
    description: '已派生下一次提醒的记录。',
    value: `${nextReminderCount.value} 条`
  },
  {
    title: '异常线索',
    description: '带严重程度的观察记录。',
    value: `${abnormalCount.value} 条`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminHealthRecords({
      recordType: filters.recordType,
      petId: filters.petId,
      operatorUserId: filters.operatorUserId,
      keyword: filters.keyword
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '健康记录加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminHealthRecordSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminHealthRecord(record.health_record.health_record_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '健康记录详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function recordTypeLabel(recordType: HealthRecordType) {
  const labelMap: Record<HealthRecordType, string> = {
    vaccine: '疫苗',
    deworming: '驱虫',
    examination: '体检',
    medication: '用药',
    weight: '体重',
    observation: '异常观察'
  };
  return labelMap[recordType];
}

function recordTypeTagType(recordType: HealthRecordType) {
  if (recordType === 'observation') {
    return 'warning';
  }
  if (recordType === 'vaccine' || recordType === 'deworming') {
    return 'success';
  }
  if (recordType === 'medication') {
    return 'danger';
  }
  return 'info';
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗'
  };
  return labelMap[petType] ?? petType;
}

function reminderStatusLabel(status: string | null) {
  if (!status) {
    return '已创建';
  }
  const labelMap: Record<string, string> = {
    pending: '待提醒',
    completed: '已完成',
    skipped: '已跳过'
  };
  return labelMap[status] ?? status;
}

function severityLabel(severity: string) {
  const labelMap: Record<string, string> = {
    low: '轻微',
    medium: '中等',
    high: '严重'
  };
  return labelMap[severity] ?? severity;
}

function mediaTypeLabel(mediaType: MediaType) {
  const labelMap: Record<MediaType, string> = {
    image: '图片',
    video: '视频',
    file: '文件'
  };
  return labelMap[mediaType];
}

function valueLabel(value: string | null, unit: string | null) {
  if (!value && !unit) {
    return '-';
  }
  return `${value ?? ''}${unit ? ` ${unit}` : ''}`.trim();
}

function formatFileSize(fileSize: number) {
  if (fileSize < 1024) {
    return `${fileSize} B`;
  }
  if (fileSize < 1024 * 1024) {
    return `${(fileSize / 1024).toFixed(1)} KB`;
  }
  return `${(fileSize / 1024 / 1024).toFixed(1)} MB`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.health-summary,
.health-section {
  margin-top: 24px;
}

.health-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.health-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.health-filter {
  width: 132px;
}

.health-keyword {
  width: 220px;
}

.health-error {
  margin-bottom: 16px;
}

.health-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.health-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.health-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.health-cell__meta,
.health-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.health-cell__detail {
  color: var(--pet-admin-body);
}

.health-count-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.health-count-stack strong {
  color: var(--pet-admin-title);
  font-size: 18px;
}

.health-count-stack span {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.health-detail {
  min-height: 320px;
}

.health-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.health-detail__section:first-child {
  padding-top: 0;
}

.health-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.health-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.health-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.health-detail-list div {
  display: grid;
  gap: 5px;
}

.health-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.health-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.health-empty-inline {
  padding: 16px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.health-asset-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.health-asset {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface);
}

.health-asset__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
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
  .health-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .health-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .health-filter,
  .health-keyword {
    width: 100%;
  }

  .health-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
