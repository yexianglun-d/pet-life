<template>
  <section class="page-section reminder-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">系统提醒</p>
      <h1 class="page-section__title">看清每一条照护提醒的状态、归属和来源</h1>
      <p class="page-section__description">
        系统提醒查询页只承接真实提醒排查，不替用户完成、跳过或改写提醒。运营侧重点是定位待处理提醒、逾期风险、宠物家庭归属和来源健康记录。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">提醒 {{ records.length }} 条</span>
        <span class="pet-admin-chip">待处理 {{ pendingReminderCount }} 条</span>
        <span class="pet-admin-chip">已逾期 {{ overdueReminderCount }} 条</span>
        <span class="pet-admin-chip">有来源记录 {{ sourceRecordCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid reminder-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel reminder-section">
      <div class="reminder-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">提醒查询队列</h2>
          <p class="pet-admin-panel__description">
            按状态、类型、模式、宠物、家庭、处理人、来源记录和提醒时间筛选，结果来自管理端系统提醒接口。
          </p>
        </div>
        <div class="reminder-toolbar__actions">
          <el-select v-model="filters.status" size="small" class="reminder-filter" placeholder="状态">
            <el-option label="全部状态" value="all" />
            <el-option
              v-for="option in reminderStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.reminderType" size="small" class="reminder-filter" placeholder="提醒类型">
            <el-option label="全部类型" value="all" />
            <el-option
              v-for="option in reminderTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.reminderMode" size="small" class="reminder-filter" placeholder="提醒模式">
            <el-option label="全部模式" value="all" />
            <el-option label="单次" value="single" />
            <el-option label="周期" value="cycle" />
          </el-select>
          <el-input v-model="filters.petId" size="small" class="reminder-filter" placeholder="宠物 ID" clearable />
          <el-input v-model="filters.familyId" size="small" class="reminder-filter" placeholder="家庭 ID" clearable />
          <el-input v-model="filters.handlerUserId" size="small" class="reminder-filter" placeholder="处理人 ID" clearable />
          <el-date-picker
            v-model="filters.dueFrom"
            type="datetime"
            size="small"
            class="reminder-date"
            placeholder="提醒开始"
            value-format="YYYY-MM-DDTHH:mm:ssZ"
          />
          <el-date-picker
            v-model="filters.dueTo"
            type="datetime"
            size="small"
            class="reminder-date"
            placeholder="提醒截止"
            value-format="YYYY-MM-DDTHH:mm:ssZ"
          />
          <el-input v-model="filters.keyword" size="small" class="reminder-keyword" placeholder="标题 / 宠物 / 家庭 / 主人 / 来源" clearable />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="reminder-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="reminder.reminder_id"
        empty-text="暂无系统提醒"
        class="reminder-table"
      >
        <el-table-column label="提醒" min-width="280">
          <template #default="{ row }">
            <div class="reminder-cell">
              <div class="reminder-cell__title">
                <span>{{ row.reminder.title }}</span>
                <el-tag size="small" :type="reminderTypeTagType(row.reminder.reminder_type)">
                  {{ reminderTypeLabel(row.reminder.reminder_type) }}
                </el-tag>
              </div>
              <div class="reminder-cell__meta">
                {{ reminderModeLabel(row.reminder.reminder_mode) }}
                <span v-if="row.reminder.reminder_mode === 'cycle'"> · {{ cycleLabel(row.reminder) }}</span>
              </div>
              <div class="reminder-cell__detail">{{ row.reminder.notes || '暂无备注' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态与时间" min-width="190">
          <template #default="{ row }">
            <div class="reminder-cell">
              <div class="reminder-cell__title">
                <el-tag size="small" :type="reminderStatusTagType(row.reminder.status)">
                  {{ reminderStatusLabel(row.reminder.status) }}
                </el-tag>
                <el-tag v-if="isReminderOverdue(row)" size="small" type="danger">已逾期</el-tag>
              </div>
              <div class="reminder-cell__meta">提醒：{{ formatDateTime(row.reminder.due_at) }}</div>
              <div class="reminder-cell__meta">完成：{{ formatNullableDateTime(row.reminder.completed_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="宠物归属" min-width="250">
          <template #default="{ row }">
            <div class="reminder-cell">
              <div class="reminder-cell__title">{{ row.pet.pet_name }}</div>
              <div class="reminder-cell__meta">
                {{ petTypeLabel(row.pet.pet_type) }} · {{ row.pet.family_name || '暂无家庭' }}
              </div>
              <div class="reminder-cell__meta">
                主人：{{ row.pet.owner_nickname || '未知主人' }}
                <span v-if="row.pet.owner_mobile">· {{ row.pet.owner_mobile }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="处理与来源" min-width="240">
          <template #default="{ row }">
            <div class="reminder-cell">
              <div class="reminder-cell__meta">
                处理人：{{ row.handler?.nickname || '暂无处理人' }}
                <span v-if="row.handler?.mobile">· {{ row.handler.mobile }}</span>
              </div>
              <template v-if="row.source_record">
                <div class="reminder-cell__title">
                  <span>{{ row.source_record.title || '来源健康记录' }}</span>
                  <el-tag size="small" :type="sourceStatusTagType(row.source_record.status)">
                    {{ sourceStatusLabel(row.source_record.status) }}
                  </el-tag>
                </div>
                <div class="reminder-cell__meta">
                  {{ sourceRecordTypeLabel(row.source_record.record_type) }} · ID {{ row.source_record.source_record_id }}
                </div>
              </template>
              <span v-else class="reminder-cell__meta">无来源健康记录</span>
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

    <el-drawer v-model="detailDrawerVisible" title="系统提醒详情" size="540px">
      <div v-loading="detailLoading" class="reminder-detail">
        <template v-if="activeRecord">
          <section class="reminder-detail__section">
            <h3>{{ activeRecord.reminder.title }}</h3>
            <div class="reminder-detail__tags">
              <el-tag :type="reminderTypeTagType(activeRecord.reminder.reminder_type)">
                {{ reminderTypeLabel(activeRecord.reminder.reminder_type) }}
              </el-tag>
              <el-tag :type="reminderStatusTagType(activeRecord.reminder.status)">
                {{ reminderStatusLabel(activeRecord.reminder.status) }}
              </el-tag>
              <el-tag v-if="isReminderOverdue(activeRecord)" type="danger">已逾期</el-tag>
            </div>
            <dl class="reminder-detail-list">
              <div>
                <dt>提醒 ID</dt>
                <dd>{{ activeRecord.reminder.reminder_id }}</dd>
              </div>
              <div>
                <dt>提醒模式</dt>
                <dd>{{ reminderModeLabel(activeRecord.reminder.reminder_mode) }} · {{ cycleLabel(activeRecord.reminder) }}</dd>
              </div>
              <div>
                <dt>提醒时间</dt>
                <dd>{{ formatDateTime(activeRecord.reminder.due_at) }}</dd>
              </div>
              <div>
                <dt>完成时间</dt>
                <dd>{{ formatNullableDateTime(activeRecord.reminder.completed_at) }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ activeRecord.reminder.notes || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="reminder-detail__section">
            <h3>宠物与家庭</h3>
            <dl class="reminder-detail-list">
              <div>
                <dt>宠物</dt>
                <dd>{{ activeRecord.pet.pet_name }} · {{ petTypeLabel(activeRecord.pet.pet_type) }}</dd>
              </div>
              <div>
                <dt>家庭</dt>
                <dd>{{ activeRecord.pet.family_name || '-' }} · ID {{ activeRecord.pet.family_id || '-' }}</dd>
              </div>
              <div>
                <dt>主人</dt>
                <dd>{{ activeRecord.pet.owner_nickname || '-' }} · {{ activeRecord.pet.owner_mobile || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="reminder-detail__section">
            <h3>处理人与来源</h3>
            <dl class="reminder-detail-list">
              <div>
                <dt>处理人</dt>
                <dd>
                  <template v-if="activeRecord.handler">
                    {{ activeRecord.handler.nickname || '-' }} · {{ activeRecord.handler.mobile || '-' }}
                  </template>
                  <template v-else>-</template>
                </dd>
              </div>
              <div>
                <dt>来源健康记录</dt>
                <dd>
                  <template v-if="activeRecord.source_record">
                    {{ activeRecord.source_record.title || '-' }} ·
                    {{ sourceRecordTypeLabel(activeRecord.source_record.record_type) }} ·
                    {{ sourceStatusLabel(activeRecord.source_record.status) }}
                  </template>
                  <template v-else>-</template>
                </dd>
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
  getAdminReminder,
  listAdminReminders,
  type AdminReminderSnapshot,
  type AdminReminderSourceStatus,
  type HealthReminderSourceType,
  type ReminderMode,
  type ReminderModeFilter,
  type ReminderSnapshot,
  type ReminderStatus,
  type ReminderStatusFilter,
  type ReminderType,
  type ReminderTypeFilter
} from '@/shared/api/adminReminderApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const reminderTypeOptions: Array<{ label: string; value: ReminderType }> = [
  { label: '疫苗', value: 'vaccine' },
  { label: '驱虫', value: 'deworming' },
  { label: '体检', value: 'examination' },
  { label: '用药', value: 'medication' },
  { label: '自定义', value: 'custom' }
];

const reminderStatusOptions: Array<{ label: string; value: ReminderStatus }> = [
  { label: '待处理', value: 'pending' },
  { label: '已完成', value: 'completed' },
  { label: '已跳过', value: 'skipped' }
];

const records = ref<AdminReminderSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminReminderSnapshot | null>(null);

const filters = reactive<{
  keyword: string;
  status: ReminderStatusFilter;
  reminderType: ReminderTypeFilter;
  reminderMode: ReminderModeFilter;
  petId: string;
  familyId: string;
  handlerUserId: string;
  dueFrom: string;
  dueTo: string;
}>({
  keyword: '',
  status: 'all',
  reminderType: 'all',
  reminderMode: 'all',
  petId: '',
  familyId: '',
  handlerUserId: '',
  dueFrom: '',
  dueTo: ''
});

const pendingReminderCount = computed(
  () => records.value.filter((record) => record.reminder.status === 'pending').length
);
const overdueReminderCount = computed(
  () => records.value.filter((record) => isReminderOverdue(record)).length
);
const cycleReminderCount = computed(
  () => records.value.filter((record) => record.reminder.reminder_mode === 'cycle').length
);
const sourceRecordCount = computed(
  () => records.value.filter((record) => record.source_record !== null).length
);

const summaryCards = computed(() => [
  {
    title: '提醒总数',
    description: '当前筛选条件下返回的系统提醒。',
    value: `${records.value.length} 条`
  },
  {
    title: '待处理',
    description: '仍处于 pending 状态的提醒。',
    value: `${pendingReminderCount.value} 条`
  },
  {
    title: '逾期风险',
    description: '待处理且提醒时间早于当前时间的提醒。',
    value: `${overdueReminderCount.value} 条`
  },
  {
    title: '周期提醒',
    description: '按周期规则生成的照护提醒。',
    value: `${cycleReminderCount.value} 条`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminReminders({
      keyword: filters.keyword,
      status: filters.status,
      reminderType: filters.reminderType,
      reminderMode: filters.reminderMode,
      petId: filters.petId,
      familyId: filters.familyId,
      handlerUserId: filters.handlerUserId,
      dueFrom: filters.dueFrom,
      dueTo: filters.dueTo
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '系统提醒加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminReminderSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminReminder(record.reminder.reminder_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '系统提醒详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function isReminderOverdue(record: AdminReminderSnapshot) {
  return record.reminder.status === 'pending' && new Date(record.reminder.due_at).getTime() < Date.now();
}

function reminderTypeLabel(reminderType: ReminderType) {
  const labelMap: Record<ReminderType, string> = {
    vaccine: '疫苗',
    deworming: '驱虫',
    examination: '体检',
    medication: '用药',
    custom: '自定义'
  };
  return labelMap[reminderType];
}

function reminderTypeTagType(reminderType: ReminderType) {
  if (reminderType === 'vaccine' || reminderType === 'deworming') {
    return 'success';
  }
  if (reminderType === 'medication') {
    return 'danger';
  }
  if (reminderType === 'custom') {
    return 'warning';
  }
  return 'info';
}

function reminderStatusLabel(status: ReminderStatus) {
  const labelMap: Record<ReminderStatus, string> = {
    pending: '待处理',
    completed: '已完成',
    skipped: '已跳过'
  };
  return labelMap[status];
}

function reminderStatusTagType(status: ReminderStatus) {
  if (status === 'pending') {
    return 'warning';
  }
  if (status === 'completed') {
    return 'success';
  }
  return 'info';
}

function reminderModeLabel(mode: ReminderMode) {
  return mode === 'cycle' ? '周期提醒' : '单次提醒';
}

function cycleLabel(reminder: ReminderSnapshot) {
  if (reminder.reminder_mode === 'single') {
    return '不重复';
  }
  if (!reminder.cycle_value || !reminder.cycle_unit) {
    return '周期未配置';
  }
  return `每 ${reminder.cycle_value} ${unitLabel(reminder.cycle_unit)}`;
}

function unitLabel(unit: string) {
  const labelMap: Record<string, string> = {
    day: '天',
    week: '周',
    month: '月'
  };
  return labelMap[unit] ?? unit;
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗',
    other: '其他'
  };
  return labelMap[petType] ?? petType;
}

function sourceRecordTypeLabel(recordType: HealthReminderSourceType | null) {
  if (!recordType) {
    return '未知类型';
  }
  const labelMap: Record<HealthReminderSourceType, string> = {
    vaccine: '疫苗记录',
    deworming: '驱虫记录',
    examination: '体检记录',
    medication: '用药记录',
    weight: '体重记录',
    observation: '异常观察'
  };
  return labelMap[recordType];
}

function sourceStatusLabel(status: AdminReminderSourceStatus) {
  const labelMap: Record<AdminReminderSourceStatus, string> = {
    active: '正常',
    deleted: '已删除',
    missing: '缺失'
  };
  return labelMap[status];
}

function sourceStatusTagType(status: AdminReminderSourceStatus) {
  if (status === 'active') {
    return 'success';
  }
  if (status === 'missing') {
    return 'warning';
  }
  return 'info';
}

function formatNullableDateTime(value: string | null) {
  return value ? formatDateTime(value) : '-';
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.reminder-summary,
.reminder-section {
  margin-top: 24px;
}

.reminder-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.reminder-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.reminder-filter {
  width: 132px;
}

.reminder-date {
  width: 178px;
}

.reminder-keyword {
  width: 260px;
}

.reminder-error {
  margin-bottom: 16px;
}

.reminder-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.reminder-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.reminder-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.reminder-cell__meta,
.reminder-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.reminder-cell__detail {
  color: var(--pet-admin-body);
}

.reminder-detail {
  min-height: 320px;
}

.reminder-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.reminder-detail__section:first-child {
  padding-top: 0;
}

.reminder-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.reminder-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.reminder-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.reminder-detail-list div {
  display: grid;
  gap: 5px;
}

.reminder-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.reminder-detail-list dd {
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
:deep(.el-date-editor),
:deep(.el-drawer) {
  border-radius: 14px;
}

:deep(.el-drawer__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .reminder-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .reminder-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .reminder-filter,
  .reminder-date,
  .reminder-keyword {
    width: 100%;
  }

  .reminder-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
