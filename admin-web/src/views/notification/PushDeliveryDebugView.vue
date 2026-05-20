<template>
  <section class="page-section push-debug-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">Push 投递排查</p>
      <h1 class="page-section__title">查看 Push 任务和设备投递记录，定位底座状态</h1>
      <p class="page-section__description">
        当前页面只展示服务端 Push 任务与投递记录底座。真实 APNs、厂商推送和供应商控制台尚未接入，sent 仅表示服务端状态标记。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">任务 {{ tasks.length }} 条</span>
        <span class="pet-admin-chip">投递 {{ deliveries.length }} 条</span>
        <span class="pet-admin-chip">失败 {{ failedRowCount }} 条</span>
        <span class="pet-admin-chip">占位 {{ devNoopRowCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid push-debug-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <el-alert
      title="Push 边界"
      description="这里是供应商无关 Push 底座排查页。dev_noop 不代表真实 APNs 或厂商通道；列表中的 sent 只表示服务端状态标记，不代表第三方实际送达。"
      type="warning"
      show-icon
      class="push-debug-boundary"
      :closable="false"
    />

    <article class="pet-admin-panel push-debug-section">
      <div class="push-debug-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">任务与投递记录</h2>
          <p class="pet-admin-panel__description">
            用户、任务状态、投递状态和供应商筛选由后台接口支持；notify_type 和时间范围基于真实返回字段在页面内收窄。
          </p>
        </div>
        <div class="push-debug-toolbar__actions">
          <el-input v-model="filters.userId" size="small" class="push-debug-filter" placeholder="用户 ID" clearable />
          <el-input
            v-model="filters.notifyType"
            size="small"
            class="push-debug-filter"
            placeholder="notify_type"
            clearable
          />
          <el-input
            v-model="filters.providerCode"
            size="small"
            class="push-debug-filter"
            placeholder="provider_code"
            clearable
          />
          <el-select v-model="filters.taskStatus" size="small" class="push-debug-filter" placeholder="任务状态">
            <el-option label="全部任务" value="all" />
            <el-option label="待处理" value="pending" />
            <el-option label="已跳过" value="skipped" />
            <el-option label="失败" value="failed" />
            <el-option label="已标记发送" value="sent" />
          </el-select>
          <el-select v-model="filters.deliveryStatus" size="small" class="push-debug-filter" placeholder="投递状态">
            <el-option label="全部投递" value="all" />
            <el-option label="待处理" value="pending" />
            <el-option label="已跳过" value="skipped" />
            <el-option label="失败" value="failed" />
            <el-option label="已标记发送" value="sent" />
          </el-select>
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            size="small"
            class="push-debug-range"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-button :loading="isLoading" @click="loadPage">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="push-debug-error"
        :closable="false"
      />

      <el-table
        :data="visibleRows"
        v-loading="isLoading"
        row-key="rowId"
        empty-text="暂无 Push 任务或投递记录"
        class="push-debug-table"
      >
        <el-table-column label="通知任务" min-width="300">
          <template #default="{ row }">
            <div class="push-debug-cell">
              <div class="push-debug-cell__title">
                <span>{{ row.task?.title || '缺少任务快照' }}</span>
                <el-tag v-if="row.task" size="small" type="info">{{ row.task.notify_type }}</el-tag>
                <el-tag v-if="row.task" size="small" :type="taskStatusTagType(row.task.task_status)">
                  {{ taskStatusLabel(row.task.task_status) }}
                </el-tag>
              </div>
              <div class="push-debug-cell__detail">{{ row.task?.content || '投递记录未关联到当前任务返回结果' }}</div>
              <div class="push-debug-cell__meta">任务 ID：{{ row.pushTaskId }}</div>
              <div v-if="row.task?.notification_id" class="push-debug-cell__meta">
                通知 ID：{{ row.task.notification_id }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户 / 设备" min-width="240">
          <template #default="{ row }">
            <div class="push-debug-cell">
              <div class="push-debug-cell__title">用户 ID：{{ row.userId }}</div>
              <div class="push-debug-cell__meta">
                设备 Token 标识：{{ maskedDeviceTokenId(row.delivery?.device_token_id) }}
              </div>
              <div class="push-debug-cell__meta">接口仅返回 device_token_id，不返回设备 token 原文。</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="供应商 / 投递" min-width="230">
          <template #default="{ row }">
            <div class="push-debug-cell">
              <div class="push-debug-cell__title">
                <el-tag :type="providerTagType(row.providerCode)">
                  {{ providerLabel(row.providerCode) }}
                </el-tag>
                <el-tag v-if="row.delivery" :type="deliveryStatusTagType(row.delivery.delivery_status)">
                  {{ deliveryStatusLabel(row.delivery.delivery_status) }}
                </el-tag>
              </div>
              <div class="push-debug-cell__detail">
                {{ row.delivery?.failure_reason || row.task?.failure_reason || '暂无失败原因' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="业务" min-width="190">
          <template #default="{ row }">
            <div class="push-debug-cell">
              <div class="push-debug-cell__meta">biz_type：{{ row.task?.biz_type || '-' }}</div>
              <div class="push-debug-cell__meta">biz_id：{{ row.task?.biz_id || '-' }}</div>
              <div class="push-debug-cell__meta">记录 ID：{{ row.delivery?.delivery_record_id || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="push-debug-cell">
              <div class="push-debug-cell__meta">任务：{{ formatDateTime(row.task?.created_at || null) }}</div>
              <div class="push-debug-cell__meta">投递：{{ formatDateTime(row.delivery?.created_at || null) }}</div>
              <div class="push-debug-cell__meta">尝试：{{ formatDateTime(row.delivery?.attempted_at || null) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="Push 排查详情" size="600px">
      <div v-if="activeRow" class="push-debug-detail">
        <el-alert
          title="投递边界"
          description="当前记录来自服务端 Push 底座，不连接供应商控制台；sent 也不代表真实第三方投递成功。"
          type="info"
          show-icon
          :closable="false"
        />

        <section class="push-debug-detail__section">
          <div class="push-debug-detail__heading">
            <h3>任务信息</h3>
            <el-tag v-if="activeRow.task" :type="taskStatusTagType(activeRow.task.task_status)">
              {{ taskStatusLabel(activeRow.task.task_status) }}
            </el-tag>
          </div>
          <dl class="push-debug-detail-list">
            <div>
              <dt>任务 ID</dt>
              <dd>{{ activeRow.pushTaskId }}</dd>
            </div>
            <div>
              <dt>通知 ID</dt>
              <dd>{{ activeRow.task?.notification_id || '-' }}</dd>
            </div>
            <div>
              <dt>notify_type</dt>
              <dd>{{ activeRow.task?.notify_type || '-' }}</dd>
            </div>
            <div>
              <dt>标题</dt>
              <dd>{{ activeRow.task?.title || '-' }}</dd>
            </div>
            <div>
              <dt>内容</dt>
              <dd>{{ activeRow.task?.content || '-' }}</dd>
            </div>
            <div>
              <dt>业务对象</dt>
              <dd>{{ activeRow.task?.biz_type || '-' }} / {{ activeRow.task?.biz_id || '-' }}</dd>
            </div>
            <div>
              <dt>失败原因</dt>
              <dd>{{ activeRow.task?.failure_reason || '-' }}</dd>
            </div>
            <div>
              <dt>创建 / 更新</dt>
              <dd>
                {{ formatDateTime(activeRow.task?.created_at || null) }} /
                {{ formatDateTime(activeRow.task?.updated_at || null) }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="push-debug-detail__section">
          <div class="push-debug-detail__heading">
            <h3>投递记录</h3>
            <el-tag v-if="activeRow.delivery" :type="deliveryStatusTagType(activeRow.delivery.delivery_status)">
              {{ deliveryStatusLabel(activeRow.delivery.delivery_status) }}
            </el-tag>
          </div>
          <dl class="push-debug-detail-list">
            <div>
              <dt>投递记录 ID</dt>
              <dd>{{ activeRow.delivery?.delivery_record_id || '-' }}</dd>
            </div>
            <div>
              <dt>用户 ID</dt>
              <dd>{{ activeRow.userId }}</dd>
            </div>
            <div>
              <dt>设备 Token 标识</dt>
              <dd>{{ maskedDeviceTokenId(activeRow.delivery?.device_token_id) }}</dd>
            </div>
            <div>
              <dt>供应商</dt>
              <dd>{{ providerLabel(activeRow.providerCode) }}</dd>
            </div>
            <div>
              <dt>失败原因</dt>
              <dd>{{ activeRow.delivery?.failure_reason || '-' }}</dd>
            </div>
            <div>
              <dt>创建 / 尝试</dt>
              <dd>
                {{ formatDateTime(activeRow.delivery?.created_at || null) }} /
                {{ formatDateTime(activeRow.delivery?.attempted_at || null) }}
              </dd>
            </div>
          </dl>
        </section>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import {
  listAdminPushDeliveries,
  listAdminPushTasks,
  type PushDeliveryRecordSnapshot,
  type PushDeliveryStatus,
  type PushDeliveryStatusFilter,
  type PushTaskSnapshot,
  type PushTaskStatus,
  type PushTaskStatusFilter
} from '@/shared/api/adminPushNotificationApi';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

interface PushDebugRow {
  rowId: string;
  pushTaskId: string;
  userId: string;
  providerCode: string;
  task: PushTaskSnapshot | null;
  delivery: PushDeliveryRecordSnapshot | null;
}

const tasks = ref<PushTaskSnapshot[]>([]);
const deliveries = ref<PushDeliveryRecordSnapshot[]>([]);
const isLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRow = ref<PushDebugRow | null>(null);

const filters = reactive<{
  userId: string;
  notifyType: string;
  providerCode: string;
  taskStatus: PushTaskStatusFilter;
  deliveryStatus: PushDeliveryStatusFilter;
  timeRange: string[] | null;
}>({
  userId: '',
  notifyType: '',
  providerCode: '',
  taskStatus: 'all',
  deliveryStatus: 'all',
  timeRange: []
});

const taskRows = computed(() => buildRows());
const visibleRows = computed(() =>
  taskRows.value.filter((row) => matchesNotifyType(row) && matchesTimeRange(row))
);
const failedRowCount = computed(
  () =>
    visibleRows.value.filter(
      (row) => row.task?.task_status === 'failed' || row.delivery?.delivery_status === 'failed'
    ).length
);
const pendingRowCount = computed(
  () =>
    visibleRows.value.filter(
      (row) => row.task?.task_status === 'pending' || row.delivery?.delivery_status === 'pending'
    ).length
);
const skippedRowCount = computed(
  () =>
    visibleRows.value.filter(
      (row) => row.task?.task_status === 'skipped' || row.delivery?.delivery_status === 'skipped'
    ).length
);
const devNoopRowCount = computed(() => visibleRows.value.filter((row) => row.providerCode === 'dev_noop').length);
const summaryCards = computed(() => [
  {
    title: '排查行',
    description: '当前筛选条件下的任务与投递记录合并行。',
    value: `${visibleRows.value.length} 行`
  },
  {
    title: '待处理',
    description: '仍处于 pending 的任务或投递记录。',
    value: `${pendingRowCount.value} 条`
  },
  {
    title: '已跳过',
    description: '因用户通知开关或无设备等原因跳过的记录。',
    value: `${skippedRowCount.value} 条`
  },
  {
    title: '失败记录',
    description: '任务或投递记录处于 failed 的数量。',
    value: `${failedRowCount.value} 条`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const [taskList, deliveryList] = await Promise.all([
      listAdminPushTasks({
        userId: filters.userId,
        taskStatus: filters.taskStatus,
        providerCode: filters.providerCode
      }),
      listAdminPushDeliveries({
        userId: filters.userId,
        deliveryStatus: filters.deliveryStatus,
        providerCode: filters.providerCode
      })
    ]);
    tasks.value = taskList;
    deliveries.value = deliveryList;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Push 任务与投递记录加载失败';
  } finally {
    isLoading.value = false;
  }
}

function buildRows(): PushDebugRow[] {
  const rows: PushDebugRow[] = [];
  const taskMap = new Map(tasks.value.map((task) => [task.push_task_id, task]));
  const taskIdsWithDelivery = new Set<string>();

  deliveries.value.forEach((delivery) => {
    const task = taskMap.get(delivery.push_task_id) ?? null;
    if (!matchesTaskOnlyFilters(task)) {
      return;
    }
    taskIdsWithDelivery.add(delivery.push_task_id);
    rows.push({
      rowId: `delivery-${delivery.delivery_record_id}`,
      pushTaskId: delivery.push_task_id,
      userId: delivery.user_id,
      providerCode: delivery.provider_code,
      task,
      delivery
    });
  });

  if (filters.deliveryStatus === 'all') {
    tasks.value
      .filter((task) => !taskIdsWithDelivery.has(task.push_task_id))
      .forEach((task) => {
        rows.push({
          rowId: `task-${task.push_task_id}`,
          pushTaskId: task.push_task_id,
          userId: task.user_id,
          providerCode: task.provider_code,
          task,
          delivery: null
        });
      });
  }

  return rows;
}

function matchesTaskOnlyFilters(task: PushTaskSnapshot | null) {
  if (!task && (filters.taskStatus !== 'all' || filters.notifyType.trim())) {
    return false;
  }
  return true;
}

function matchesNotifyType(row: PushDebugRow) {
  const notifyType = filters.notifyType.trim().toLowerCase();
  if (!notifyType) {
    return true;
  }
  return row.task?.notify_type.toLowerCase().includes(notifyType) ?? false;
}

function matchesTimeRange(row: PushDebugRow) {
  const range = filters.timeRange;
  if (!Array.isArray(range) || range.length !== 2) {
    return true;
  }
  const rowTime = row.delivery?.attempted_at || row.delivery?.created_at || row.task?.created_at;
  if (!rowTime) {
    return false;
  }
  const recordTime = new Date(rowTime).getTime();
  const startTime = new Date(range[0]).getTime();
  const endTime = new Date(range[1]).getTime();
  return recordTime >= startTime && recordTime <= endTime;
}

function openDetail(row: PushDebugRow) {
  activeRow.value = row;
  detailDrawerVisible.value = true;
}

function maskedDeviceTokenId(value: string | undefined) {
  if (!value) {
    return '-';
  }
  if (value.length <= 8) {
    return `${value.slice(0, 2)}****${value.slice(-2)}`;
  }
  return `${value.slice(0, 4)}****${value.slice(-4)}`;
}

function providerLabel(providerCode: string) {
  if (providerCode === 'dev_noop') {
    return 'dev_noop · 本地占位';
  }
  return providerCode;
}

function providerTagType(providerCode: string): ElementTagType {
  return providerCode === 'dev_noop' ? 'info' : 'primary';
}

function taskStatusLabel(status: PushTaskStatus) {
  const labelMap: Record<PushTaskStatus, string> = {
    pending: '待处理',
    skipped: '已跳过',
    failed: '失败',
    sent: '已标记发送'
  };
  return labelMap[status];
}

function taskStatusTagType(status: PushTaskStatus): ElementTagType {
  if (status === 'sent') {
    return 'info';
  }
  if (status === 'failed') {
    return 'danger';
  }
  if (status === 'skipped') {
    return 'warning';
  }
  return 'primary';
}

function deliveryStatusLabel(status: PushDeliveryStatus) {
  const labelMap: Record<PushDeliveryStatus, string> = {
    pending: '待处理',
    skipped: '已跳过',
    failed: '失败',
    sent: '已标记发送'
  };
  return labelMap[status];
}

function deliveryStatusTagType(status: PushDeliveryStatus): ElementTagType {
  if (status === 'sent') {
    return 'info';
  }
  if (status === 'failed') {
    return 'danger';
  }
  if (status === 'skipped') {
    return 'warning';
  }
  return 'primary';
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.push-debug-summary,
.push-debug-section,
.push-debug-boundary {
  margin-top: 24px;
}

.push-debug-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.push-debug-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.push-debug-filter {
  width: 142px;
}

.push-debug-range {
  width: 310px;
}

.push-debug-error {
  margin-bottom: 16px;
}

.push-debug-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.push-debug-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.push-debug-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.push-debug-cell__meta,
.push-debug-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.push-debug-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.push-debug-detail__section {
  padding: 18px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 20px;
  background: var(--pet-admin-surface-soft);
}

.push-debug-detail__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.push-debug-detail__heading h3 {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 16px;
}

.push-debug-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.push-debug-detail-list div {
  display: grid;
  grid-template-columns: 108px 1fr;
  gap: 12px;
}

.push-debug-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.push-debug-detail-list dd {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 13px;
  word-break: break-all;
}

:deep(.el-button),
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 14px;
}

@media (max-width: 1100px) {
  .push-debug-toolbar {
    flex-direction: column;
  }

  .push-debug-toolbar__actions {
    justify-content: flex-start;
  }
}
</style>
