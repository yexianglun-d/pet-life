<template>
  <section class="page-section moderation-task-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">内容审核任务</p>
      <h1 class="page-section__title">内容审核任务</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">任务 {{ visibleTasks.length }} 条</span>
        <span class="pet-admin-chip">待处理 {{ pendingCount }} 条</span>
        <span class="pet-admin-chip">已通过 {{ approvedCount }} 条</span>
        <span class="pet-admin-chip">已拒绝 {{ rejectedCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid moderation-task-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <el-alert
      title="功能未完成：缺少真实内容审核供应商"
      type="warning"
      show-icon
      class="moderation-task-boundary"
      :closable="false"
    />

    <article class="pet-admin-panel moderation-task-section">
      <div class="moderation-task-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">审核任务列表</h2>
        </div>
        <div class="moderation-task-toolbar__actions">
          <el-select v-model="filters.targetType" size="small" class="moderation-task-filter" placeholder="目标类型">
            <el-option label="全部目标" value="all" />
            <el-option label="社区帖子" value="community_post" />
            <el-option label="社区问答" value="community_question" />
          </el-select>
          <el-select v-model="filters.contentType" size="small" class="moderation-task-filter" placeholder="内容类型">
            <el-option label="全部内容" value="all" />
            <el-option label="文本" value="text" />
            <el-option label="图文" value="image_text" />
            <el-option label="视频" value="video" />
            <el-option label="问答" value="qa" />
          </el-select>
          <el-select v-model="filters.reviewStatus" size="small" class="moderation-task-filter" placeholder="审核状态">
            <el-option label="全部状态" value="all" />
            <el-option label="待处理" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="失败" value="failed" />
          </el-select>
          <el-input
            v-model="filters.providerCode"
            size="small"
            class="moderation-task-filter"
            placeholder="provider_code"
            clearable
          />
          <el-input
            v-model="filters.keyword"
            size="small"
            class="moderation-task-keyword"
            placeholder="关键词 / 目标 ID / 任务 ID"
            clearable
          />
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            size="small"
            class="moderation-task-range"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-button :loading="isLoading || auditLogLoading" @click="loadPage">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="moderation-task-error"
        :closable="false"
      />

      <el-table
        :data="visibleTasks"
        v-loading="isLoading"
        row-key="task_id"
        empty-text="暂无内容审核任务"
        class="moderation-task-table"
      >
        <el-table-column label="审核目标" min-width="270">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__title">
                <span>{{ targetTypeLabel(row.target_type) }}</span>
                <el-tag size="small" type="info">{{ contentTypeLabel(row.content_type) }}</el-tag>
                <el-tag size="small" :type="reviewStatusTagType(row.review_status)">
                  {{ reviewStatusLabel(row.review_status) }}
                </el-tag>
              </div>
              <div class="moderation-task-cell__meta">目标 ID：{{ row.target_id }}</div>
              <div class="moderation-task-cell__meta">任务 ID：{{ row.task_id }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="内容摘要" min-width="330">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__detail">{{ contentSummary(row) }}</div>
              <div class="moderation-task-risk">
                <el-tag
                  v-for="riskLabel in riskLabels(row.risk_labels)"
                  :key="`${row.task_id}-${riskLabel}`"
                  size="small"
                  type="warning"
                >
                  {{ riskLabel }}
                </el-tag>
                <span v-if="riskLabels(row.risk_labels).length === 0" class="moderation-task-cell__meta">
                  无风险标签
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="供应商 / 失败原因" min-width="230">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__title">
                <el-tag :type="providerTagType(row.provider_code)">
                  {{ providerLabel(row.provider_code) }}
                </el-tag>
              </div>
              <div class="moderation-task-cell__detail">
                {{ row.failure_reason || '暂无失败原因' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__meta">创建：{{ formatDateTime(row.created_at) }}</div>
              <div class="moderation-task-cell__meta">更新：{{ formatDateTime(row.updated_at) }}</div>
              <div class="moderation-task-cell__meta">审核：{{ formatDateTime(row.reviewed_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="moderation-task-actions">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                size="small"
                type="success"
                :disabled="row.review_status !== 'pending'"
                :loading="processingTaskId === row.task_id && reviewForm.action === 'approve'"
                @click="openReviewDialog(row, 'approve')"
              >
                通过
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="row.review_status !== 'pending'"
                :loading="processingTaskId === row.task_id && reviewForm.action === 'reject'"
                @click="openReviewDialog(row, 'reject')"
              >
                拒绝
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel moderation-task-section">
      <div class="moderation-task-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">审核任务审计</h2>
        </div>
        <div class="moderation-task-toolbar__actions">
          <el-input
            v-model="auditLogFilters.operatorId"
            size="small"
            class="moderation-task-audit-filter"
            placeholder="操作者"
            clearable
          />
          <el-button :loading="auditLogLoading" @click="loadAuditLogs">刷新审计</el-button>
        </div>
      </div>

      <el-alert
        v-if="auditLogErrorMessage"
        :title="auditLogErrorMessage"
        type="error"
        show-icon
        class="moderation-task-error"
        :closable="false"
      />

      <el-table
        :data="auditLogs"
        v-loading="auditLogLoading"
        row-key="audit_log_id"
        empty-text="暂无审核任务审计记录"
        class="moderation-task-table"
      >
        <el-table-column label="动作" min-width="250">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__title">
                <span>{{ auditActionLabel(row.action) }}</span>
                <el-tag size="small" type="info">{{ row.target_type }}</el-tag>
              </div>
              <div class="moderation-task-cell__meta">任务 ID：{{ row.target_id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }">
            <div class="moderation-task-cell">
              <div class="moderation-task-cell__title">{{ row.operator_id }}</div>
              <div class="moderation-task-cell__meta">{{ row.ip_address || '未知 IP' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="320">
          <template #default="{ row }">
            <pre class="moderation-task-code">{{ formatJsonText(row.detail_json) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="内容审核任务详情" size="620px">
      <div v-loading="detailLoading" class="moderation-task-detail">
        <template v-if="activeTask">
          <section class="moderation-task-detail__section">
            <div class="moderation-task-detail__heading">
              <h3>{{ targetTypeLabel(activeTask.target_type) }} · #{{ activeTask.target_id }}</h3>
              <el-tag :type="reviewStatusTagType(activeTask.review_status)">
                {{ reviewStatusLabel(activeTask.review_status) }}
              </el-tag>
            </div>
            <div class="moderation-task-detail__tags">
              <el-tag type="info">{{ contentTypeLabel(activeTask.content_type) }}</el-tag>
              <el-tag :type="providerTagType(activeTask.provider_code)">
                {{ providerLabel(activeTask.provider_code) }}
              </el-tag>
            </div>
            <dl class="moderation-task-detail-list">
              <div>
                <dt>任务 ID</dt>
                <dd>{{ activeTask.task_id }}</dd>
              </div>
              <div>
                <dt>失败原因</dt>
                <dd>{{ activeTask.failure_reason || '-' }}</dd>
              </div>
              <div>
                <dt>创建 / 更新</dt>
                <dd>{{ formatDateTime(activeTask.created_at) }} / {{ formatDateTime(activeTask.updated_at) }}</dd>
              </div>
              <div>
                <dt>审核时间</dt>
                <dd>{{ formatDateTime(activeTask.reviewed_at) }}</dd>
              </div>
            </dl>
          </section>

          <section class="moderation-task-detail__section">
            <h3>风险标签</h3>
            <div class="moderation-task-risk">
              <el-tag
                v-for="riskLabel in riskLabels(activeTask.risk_labels)"
                :key="`detail-${activeTask.task_id}-${riskLabel}`"
                type="warning"
              >
                {{ riskLabel }}
              </el-tag>
              <span v-if="riskLabels(activeTask.risk_labels).length === 0" class="moderation-task-cell__meta">
                无风险标签
              </span>
            </div>
          </section>

          <section class="moderation-task-detail__section">
            <h3>审核内容快照</h3>
            <pre class="moderation-task-code">{{ formatJsonText(activeTask.content_snapshot) }}</pre>
          </section>

          <section class="moderation-task-detail__section">
            <h3>审核结果</h3>
            <pre class="moderation-task-code">{{ formatJsonText(activeTask.review_result) }}</pre>
          </section>

          <section class="moderation-task-detail__section">
            <h3>回调 Payload</h3>
            <pre class="moderation-task-code">{{ formatJsonText(activeTask.callback_payload) }}</pre>
          </section>

          <section class="moderation-task-detail__section">
            <div class="moderation-task-detail__heading">
              <h3>审计记录</h3>
              <el-button size="small" :loading="auditLogLoading" @click="loadAuditLogs">刷新</el-button>
            </div>
            <div v-if="activeTaskAuditLogs.length === 0" class="moderation-task-empty-inline">
              暂无该任务审计记录
            </div>
            <article
              v-for="auditLog in activeTaskAuditLogs"
              :key="auditLog.audit_log_id"
              class="moderation-task-audit-card"
            >
              <div class="moderation-task-cell__title">
                <span>{{ auditActionLabel(auditLog.action) }}</span>
                <span class="moderation-task-cell__meta">{{ formatDateTime(auditLog.created_at) }}</span>
              </div>
              <div class="moderation-task-cell__meta">
                {{ auditLog.operator_id }} · {{ auditLog.ip_address || '未知 IP' }}
              </div>
              <pre class="moderation-task-code">{{ formatJsonText(auditLog.detail_json) }}</pre>
            </article>
          </section>

          <section v-if="activeTask.review_status === 'pending'" class="moderation-task-detail__section">
            <div class="moderation-task-actions">
              <el-button type="success" @click="openReviewDialog(activeTask, 'approve')">人工通过</el-button>
              <el-button type="danger" @click="openReviewDialog(activeTask, 'reject')">人工拒绝</el-button>
            </div>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewDialogVisible" :title="reviewDialogTitle" width="560px">
      <div v-if="activeReviewTask" class="moderation-task-dialog-context">
        <div class="moderation-task-dialog-context__title">
          {{ targetTypeLabel(activeReviewTask.target_type) }} · #{{ activeReviewTask.target_id }}
        </div>
        <div class="moderation-task-dialog-context__meta">
          任务 ID：{{ activeReviewTask.task_id }} · 当前状态：{{ reviewStatusLabel(activeReviewTask.review_status) }}
        </div>
      </div>
      <el-form label-position="top" class="moderation-task-form">
        <el-form-item label="人工动作">
          <el-tag :type="reviewForm.action === 'approve' ? 'success' : 'danger'">
            {{ reviewActionLabel(reviewForm.action) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="风险标签">
          <el-input
            v-model="reviewForm.riskLabelText"
            placeholder="多个标签可用逗号或换行分隔，留空则提交 null"
          />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.adminNotes"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          :type="reviewForm.action === 'approve' ? 'success' : 'danger'"
          :loading="reviewSubmitting"
          @click="submitReviewForm"
        >
          {{ reviewActionLabel(reviewForm.action) }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  getAdminModerationTask,
  listAdminModerationTasks,
  listModerationAuditLogs,
  reviewAdminModerationTask,
  type ModerationAuditLogSnapshot,
  type ModerationReviewAction,
  type ModerationTaskContentType,
  type ModerationTaskContentTypeFilter,
  type ModerationTaskReviewStatus,
  type ModerationTaskReviewStatusFilter,
  type ModerationTaskSnapshot,
  type ModerationTaskTargetType,
  type ModerationTaskTargetTypeFilter
} from '@/shared/api/adminModerationTaskApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';
type JsonRecord = Record<string, unknown>;

const tasks = ref<ModerationTaskSnapshot[]>([]);
const auditLogs = ref<ModerationAuditLogSnapshot[]>([]);
const isLoading = ref(false);
const auditLogLoading = ref(false);
const detailLoading = ref(false);
const reviewSubmitting = ref(false);
const errorMessage = ref('');
const auditLogErrorMessage = ref('');
const detailDrawerVisible = ref(false);
const reviewDialogVisible = ref(false);
const activeTask = ref<ModerationTaskSnapshot | null>(null);
const activeReviewTask = ref<ModerationTaskSnapshot | null>(null);
const processingTaskId = ref<string | null>(null);

const filters = reactive<{
  targetType: ModerationTaskTargetTypeFilter;
  contentType: ModerationTaskContentTypeFilter;
  reviewStatus: ModerationTaskReviewStatusFilter;
  providerCode: string;
  keyword: string;
  timeRange: string[] | null;
}>({
  targetType: 'all',
  contentType: 'all',
  reviewStatus: 'all',
  providerCode: '',
  keyword: '',
  timeRange: []
});

const auditLogFilters = reactive<{
  operatorId: string;
}>({
  operatorId: ''
});

const reviewForm = reactive<{
  action: ModerationReviewAction;
  riskLabelText: string;
  adminNotes: string;
}>({
  action: 'approve',
  riskLabelText: '',
  adminNotes: ''
});

const visibleTasks = computed(() =>
  tasks.value.filter((task) => matchesKeyword(task) && matchesTimeRange(task))
);
const pendingCount = computed(() => visibleTasks.value.filter((task) => task.review_status === 'pending').length);
const approvedCount = computed(() => visibleTasks.value.filter((task) => task.review_status === 'approved').length);
const rejectedCount = computed(() => visibleTasks.value.filter((task) => task.review_status === 'rejected').length);
const failedCount = computed(() => visibleTasks.value.filter((task) => task.review_status === 'failed').length);
const devNoopCount = computed(() => visibleTasks.value.filter((task) => task.provider_code === 'dev_noop').length);
const activeTaskAuditLogs = computed(() => {
  if (!activeTask.value) {
    return [];
  }
  return auditLogs.value.filter((auditLog) => auditLog.target_id === activeTask.value?.task_id);
});
const reviewDialogTitle = computed(() => `${reviewActionLabel(reviewForm.action)}内容审核任务`);
const summaryCards = computed(() => [
  {
    title: '当前任务',
    description: '当前筛选条件下的审核任务数量。',
    value: `${visibleTasks.value.length} 条`
  },
  {
    title: '待处理',
    description: '仍等待人工或供应商结论的任务。',
    value: `${pendingCount.value} 条`
  },
  {
    title: '失败任务',
    description: '供应商或流程执行失败，需要排查的任务。',
    value: `${failedCount.value} 条`
  },
  {
    title: '未接供应商',
    description: 'provider_code 为 dev_noop 的本地底座任务。',
    value: `${devNoopCount.value} 条`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  await Promise.all([loadTasks(), loadAuditLogs()]);
}

async function loadTasks() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    tasks.value = await listAdminModerationTasks({
      targetType: filters.targetType,
      contentType: filters.contentType,
      reviewStatus: filters.reviewStatus,
      providerCode: filters.providerCode
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '内容审核任务加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function loadAuditLogs() {
  auditLogLoading.value = true;
  auditLogErrorMessage.value = '';
  try {
    auditLogs.value = await listModerationAuditLogs({
      targetType: 'moderation_task',
      operatorId: auditLogFilters.operatorId
    });
  } catch (error) {
    auditLogErrorMessage.value = error instanceof Error ? error.message : '审核任务审计加载失败';
  } finally {
    auditLogLoading.value = false;
  }
}

async function openDetail(task: ModerationTaskSnapshot) {
  detailDrawerVisible.value = true;
  activeTask.value = task;
  detailLoading.value = true;
  try {
    activeTask.value = await getAdminModerationTask(task.task_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '内容审核任务详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openReviewDialog(task: ModerationTaskSnapshot, action: ModerationReviewAction) {
  if (task.review_status !== 'pending') {
    ElMessage.warning('只有待处理审核任务可以人工处理');
    return;
  }
  activeReviewTask.value = task;
  reviewForm.action = action;
  reviewForm.riskLabelText = riskLabels(task.risk_labels).join(', ');
  reviewForm.adminNotes = '';
  reviewDialogVisible.value = true;
}

async function submitReviewForm() {
  if (!activeReviewTask.value) {
    return;
  }

  const task = activeReviewTask.value;
  const actionLabel = reviewActionLabel(reviewForm.action);
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}审核任务 #${task.task_id}？处理后会同步目标内容审核状态并写入审计记录。`,
      `${actionLabel}审核任务`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        type: reviewForm.action === 'approve' ? 'success' : 'warning'
      }
    );
    processingTaskId.value = task.task_id;
    reviewSubmitting.value = true;
    const updatedTask = await reviewAdminModerationTask(task.task_id, {
      action: reviewForm.action,
      risk_labels: normalizeRiskLabels(reviewForm.riskLabelText),
      admin_notes: normalizeNullableText(reviewForm.adminNotes)
    });
    tasks.value = tasks.value.map((item) => (item.task_id === updatedTask.task_id ? updatedTask : item));
    if (activeTask.value?.task_id === updatedTask.task_id) {
      activeTask.value = updatedTask;
    }
    reviewDialogVisible.value = false;
    ElMessage.success(`${actionLabel}已完成`);
    await Promise.all([loadTasks(), loadAuditLogs()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : `${actionLabel}失败`);
    }
  } finally {
    reviewSubmitting.value = false;
    processingTaskId.value = null;
  }
}

function matchesKeyword(task: ModerationTaskSnapshot) {
  const keyword = filters.keyword.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  const searchableText = [
    task.task_id,
    task.target_id,
    task.target_type,
    task.content_type,
    task.provider_code,
    task.review_status,
    task.failure_reason ?? '',
    task.content_snapshot,
    task.review_result ?? '',
    task.callback_payload ?? ''
  ]
    .join(' ')
    .toLowerCase();
  return searchableText.includes(keyword);
}

function matchesTimeRange(task: ModerationTaskSnapshot) {
  const range = filters.timeRange;
  if (!Array.isArray(range) || range.length !== 2) {
    return true;
  }
  const createdTime = new Date(task.created_at).getTime();
  const startTime = new Date(range[0]).getTime();
  const endTime = new Date(range[1]).getTime();
  return createdTime >= startTime && createdTime <= endTime;
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function normalizeRiskLabels(value: string) {
  const labels = value
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
  return labels.length > 0 ? labels : null;
}

function riskLabels(value: string | null) {
  if (!value) {
    return [];
  }
  const parsedValue = parseJsonValue(value);
  if (Array.isArray(parsedValue)) {
    return parsedValue.map((item) => String(item)).filter(Boolean);
  }
  if (typeof parsedValue === 'string') {
    return parsedValue ? [parsedValue] : [];
  }
  if (isJsonRecord(parsedValue) && Array.isArray(parsedValue.labels)) {
    return parsedValue.labels.map((item) => String(item)).filter(Boolean);
  }
  return [];
}

function contentSummary(task: ModerationTaskSnapshot) {
  const parsedValue = parseJsonValue(task.content_snapshot);
  if (isJsonRecord(parsedValue)) {
    const candidate = extractTextCandidate(parsedValue);
    return summarizeText(candidate || JSON.stringify(parsedValue), 132);
  }
  if (typeof parsedValue === 'string') {
    return summarizeText(parsedValue, 132);
  }
  return summarizeText(JSON.stringify(parsedValue), 132);
}

function extractTextCandidate(value: JsonRecord): string {
  const keys = ['title', 'content', 'text', 'summary', 'question', 'answer', 'body', 'description'];
  for (const key of keys) {
    const candidate = value[key];
    if (typeof candidate === 'string' && candidate.trim()) {
      return candidate.trim();
    }
  }
  return '';
}

function parseJsonValue(value: string | null): unknown {
  if (!value) {
    return null;
  }
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function isJsonRecord(value: unknown): value is JsonRecord {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function formatJsonText(value: string | null) {
  if (!value) {
    return '-';
  }
  const parsedValue = parseJsonValue(value);
  if (typeof parsedValue === 'string') {
    return parsedValue || '-';
  }
  return JSON.stringify(parsedValue, null, 2);
}

function targetTypeLabel(targetType: ModerationTaskTargetType) {
  const labelMap: Record<ModerationTaskTargetType, string> = {
    community_post: '社区帖子',
    community_question: '社区问答'
  };
  return labelMap[targetType];
}

function contentTypeLabel(contentType: ModerationTaskContentType) {
  const labelMap: Record<ModerationTaskContentType, string> = {
    text: '文本',
    image_text: '图文',
    video: '视频',
    qa: '问答'
  };
  return labelMap[contentType];
}

function reviewStatusLabel(status: ModerationTaskReviewStatus) {
  const labelMap: Record<ModerationTaskReviewStatus, string> = {
    pending: '待处理',
    approved: '已通过',
    rejected: '已拒绝',
    failed: '失败'
  };
  return labelMap[status];
}

function reviewStatusTagType(status: ModerationTaskReviewStatus): ElementTagType {
  if (status === 'approved') {
    return 'success';
  }
  if (status === 'pending') {
    return 'warning';
  }
  if (status === 'failed') {
    return 'info';
  }
  return 'danger';
}

function reviewActionLabel(action: ModerationReviewAction) {
  const labelMap: Record<ModerationReviewAction, string> = {
    approve: '人工通过',
    reject: '人工拒绝'
  };
  return labelMap[action];
}

function providerLabel(providerCode: string) {
  if (providerCode === 'dev_noop') {
    return 'dev_noop · 未接供应商';
  }
  if (providerCode === 'manual') {
    return 'manual · 人工处理';
  }
  return providerCode;
}

function providerTagType(providerCode: string): ElementTagType {
  if (providerCode === 'dev_noop') {
    return 'info';
  }
  if (providerCode === 'manual') {
    return 'warning';
  }
  return 'primary';
}

function auditActionLabel(action: string) {
  const labelMap: Record<string, string> = {
    moderation_task_approve: '人工通过',
    moderation_task_reject: '人工拒绝'
  };
  return labelMap[action] ?? action;
}

function summarizeText(content: string, maxLength: number) {
  return content.length > maxLength ? `${content.slice(0, maxLength)}...` : content;
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
.moderation-task-summary,
.moderation-task-section,
.moderation-task-boundary {
  margin-top: 24px;
}

.moderation-task-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.moderation-task-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.moderation-task-filter {
  width: 142px;
}

.moderation-task-keyword {
  width: 200px;
}

.moderation-task-range {
  width: 310px;
}

.moderation-task-audit-filter {
  width: 160px;
}

.moderation-task-error {
  margin-bottom: 16px;
}

.moderation-task-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.moderation-task-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.moderation-task-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.moderation-task-cell__meta,
.moderation-task-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.moderation-task-risk,
.moderation-task-actions,
.moderation-task-detail__tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.moderation-task-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.moderation-task-detail__section {
  padding: 18px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 20px;
  background: var(--pet-admin-surface-soft);
}

.moderation-task-detail__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.moderation-task-detail__heading h3,
.moderation-task-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 16px;
}

.moderation-task-detail__heading h3 {
  margin: 0;
}

.moderation-task-detail-list {
  display: grid;
  gap: 12px;
  margin: 16px 0 0;
}

.moderation-task-detail-list div {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 12px;
}

.moderation-task-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.moderation-task-detail-list dd {
  margin: 0;
  color: var(--pet-admin-title);
  font-size: 13px;
  word-break: break-all;
}

.moderation-task-code {
  max-height: 240px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--pet-admin-body);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.moderation-task-empty-inline {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.moderation-task-audit-card {
  display: grid;
  gap: 10px;
  padding: 12px 0;
  border-top: 1px solid var(--pet-admin-line);
}

.moderation-task-audit-card:first-of-type {
  border-top: none;
}

.moderation-task-dialog-context {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
}

.moderation-task-dialog-context__title {
  color: var(--pet-admin-title);
  font-weight: 700;
}

.moderation-task-dialog-context__meta {
  margin-top: 6px;
  color: var(--pet-admin-muted);
  font-size: 13px;
}

:deep(.el-button),
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 14px;
}

@media (max-width: 1100px) {
  .moderation-task-toolbar {
    flex-direction: column;
  }

  .moderation-task-toolbar__actions {
    justify-content: flex-start;
  }
}
</style>
