<template>
  <section class="page-section">
    <div class="pet-admin-hero moderation-hero">
      <p class="page-section__eyebrow">社区治理</p>
      <h1 class="page-section__title">社区举报处理</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">待处理 {{ pendingCount }} 条</span>
        <span class="pet-admin-chip">已确认 {{ processedCount }} 条</span>
        <span class="pet-admin-chip">已关闭 {{ resolvedCount }} 条</span>
      </div>
    </div>

    <div class="moderation-summary">
      <article class="summary-card">
        <h2>待处理</h2>
        <strong>{{ pendingCount }} 条</strong>
      </article>
      <article class="summary-card">
        <h2>确认违规</h2>
        <strong>{{ processedCount }} 条</strong>
      </article>
      <article class="summary-card">
        <h2>驳回举报</h2>
        <strong>{{ rejectedCount }} 条</strong>
      </article>
      <article class="summary-card">
        <h2>已结案</h2>
        <strong>{{ resolvedCount }} 条</strong>
      </article>
    </div>

    <article class="pet-admin-panel">
      <div class="moderation-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">举报队列</h2>
        </div>
        <div class="moderation-toolbar__actions">
          <el-radio-group v-model="statusFilter" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">待处理</el-radio-button>
            <el-radio-button label="processed">确认违规</el-radio-button>
            <el-radio-button label="rejected">驳回举报</el-radio-button>
          </el-radio-group>
          <el-button :loading="isLoading" @click="loadReports">刷新列表</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="moderation-error"
        :closable="false"
      />

      <el-table
        :data="visibleReports"
        v-loading="isLoading"
        empty-text="暂无举报记录"
        class="moderation-table"
      >
        <el-table-column label="举报信息" min-width="240">
          <template #default="{ row }">
            <div class="report-cell">
              <div class="report-cell__title">
                <el-tag size="small" :type="reasonTagType(row.reason_code)">
                  {{ reasonLabel(row.reason_code) }}
                </el-tag>
                <span>#{{ row.report_id }}</span>
              </div>
              <div class="report-cell__meta">
                举报人：{{ row.reporter_nickname || '未知用户' }}
                <span v-if="row.reporter_mobile">· {{ row.reporter_mobile }}</span>
              </div>
              <div v-if="row.reason_detail" class="report-cell__detail">{{ row.reason_detail }}</div>
              <div class="report-cell__meta">{{ formatDateTime(row.created_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="目标内容" min-width="340">
          <template #default="{ row }">
            <div class="report-cell">
              <div class="report-cell__title">
                <span>{{ row.post_title || '目标帖子不存在' }}</span>
                <el-tag size="small" :type="postStatusTagType(row)">
                  {{ postStatusLabel(row) }}
                </el-tag>
              </div>
              <div class="report-cell__detail">
                {{ summarizePostContent(row.post_content) }}
              </div>
              <div class="report-cell__meta">
                作者：{{ row.post_author_nickname || '未知作者' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="处理状态" width="150">
          <template #default="{ row }">
            <el-tag :type="reportStatusTagType(row.status)">
              {{ reportStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="处理信息" min-width="220">
          <template #default="{ row }">
            <div class="report-cell">
              <div class="report-cell__meta">
                {{ row.processed_by || '未处理' }}
              </div>
              <div class="report-cell__meta">
                {{ row.processed_at ? formatDateTime(row.processed_at) : '等待处理' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="moderation-actions">
              <el-button
                type="danger"
                size="small"
                :disabled="row.status !== 'pending' || processingReportId === row.report_id"
                @click="openProcessDialog(row, 'confirm_violation')"
              >
                确认违规
              </el-button>
              <el-button
                size="small"
                :disabled="row.status !== 'pending' || processingReportId === row.report_id"
                @click="openProcessDialog(row, 'dismiss_report')"
              >
                驳回举报
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-dialog v-model="processDialogVisible" :title="processDialogTitle" width="560px">
      <div v-if="activeProcessReport" class="moderation-dialog-context">
        <div class="moderation-dialog-context__title">
          #{{ activeProcessReport.report_id }} · {{ reasonLabel(activeProcessReport.reason_code) }}
        </div>
        <div class="moderation-dialog-context__meta">
          {{ activeProcessReport.post_title || '目标帖子不存在' }}
        </div>
      </div>
      <el-form label-position="top" class="moderation-form">
        <el-form-item label="处理动作">
          <el-tag :type="processForm.action === 'confirm_violation' ? 'danger' : 'info'">
            {{ processActionLabel(processForm.action) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="processForm.adminNotes"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button
          :type="processForm.action === 'confirm_violation' ? 'danger' : 'primary'"
          :loading="Boolean(processingReportId)"
          @click="submitProcessForm"
        >
          {{ processActionLabel(processForm.action) }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  listModerationReports,
  processModerationReport,
  type ModerationProcessAction,
  type ModerationReportListFilter,
  type ModerationReportSnapshot,
  type ModerationReportStatus
} from '@/shared/api/moderationApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const reports = ref<ModerationReportSnapshot[]>([]);
const isLoading = ref(false);
const processingReportId = ref<string | null>(null);
const errorMessage = ref('');
const statusFilter = ref<ModerationReportListFilter>('pending');
const processDialogVisible = ref(false);
const activeProcessReport = ref<ModerationReportSnapshot | null>(null);
const processForm = reactive<{
  action: ModerationProcessAction;
  adminNotes: string;
}>({
  action: 'dismiss_report',
  adminNotes: ''
});

const visibleReports = computed(() => {
  if (statusFilter.value === 'all') {
    return reports.value;
  }
  return reports.value.filter((report) => report.status === statusFilter.value);
});

const pendingCount = computed(() => reports.value.filter((report) => report.status === 'pending').length);
const processedCount = computed(() => reports.value.filter((report) => report.status === 'processed').length);
const rejectedCount = computed(() => reports.value.filter((report) => report.status === 'rejected').length);
const resolvedCount = computed(() => processedCount.value + rejectedCount.value);
const processDialogTitle = computed(() => `${processActionLabel(processForm.action)}举报`);

onMounted(() => {
  void loadReports();
});

async function loadReports() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    reports.value = await listModerationReports('all');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '审核列表加载失败';
  } finally {
    isLoading.value = false;
  }
}

function openProcessDialog(report: ModerationReportSnapshot, action: ModerationProcessAction) {
  activeProcessReport.value = report;
  processForm.action = action;
  processForm.adminNotes = '';
  processDialogVisible.value = true;
}

async function submitProcessForm() {
  if (!activeProcessReport.value) {
    return;
  }

  const actionLabel = processActionLabel(processForm.action);
  processingReportId.value = activeProcessReport.value.report_id;
  try {
    const updatedReport = await processModerationReport(
      activeProcessReport.value.report_id,
      processForm.action,
      normalizeNullableText(processForm.adminNotes)
    );
    reports.value = reports.value.map((item) =>
      item.report_id === updatedReport.report_id ? updatedReport : item
    );
    processDialogVisible.value = false;
    ElMessage.success(`${actionLabel}已完成`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '举报处理失败');
  } finally {
    processingReportId.value = null;
  }
}

function processActionLabel(action: ModerationProcessAction) {
  const actionLabelMap: Record<ModerationProcessAction, string> = {
    confirm_violation: '确认违规',
    dismiss_report: '驳回举报'
  };
  return actionLabelMap[action];
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function reasonLabel(reasonCode: string) {
  const reasonLabelMap: Record<string, string> = {
    spam: '广告引流',
    pornography: '低俗色情',
    harassment: '攻击辱骂',
    illegal: '违法违规',
    other: '其他原因'
  };
  return reasonLabelMap[reasonCode] ?? reasonCode;
}

function reportStatusLabel(status: ModerationReportStatus) {
  const statusLabelMap: Record<ModerationReportStatus, string> = {
    pending: '待处理',
    processed: '确认违规',
    rejected: '驳回举报'
  };
  return statusLabelMap[status];
}

function reportStatusTagType(status: ModerationReportStatus) {
  if (status === 'pending') {
    return 'warning';
  }
  if (status === 'processed') {
    return 'danger';
  }
  return 'info';
}

function reasonTagType(reasonCode: string) {
  if (reasonCode === 'illegal' || reasonCode === 'pornography') {
    return 'danger';
  }
  if (reasonCode === 'harassment') {
    return 'warning';
  }
  return 'info';
}

function postStatusLabel(report: ModerationReportSnapshot) {
  if (report.post_deleted) {
    return '内容已撤回';
  }
  if (report.post_review_status === 'rejected') {
    return '已下架';
  }
  if (report.post_review_status === 'approved') {
    return '公开中';
  }
  return '状态未知';
}

function postStatusTagType(report: ModerationReportSnapshot) {
  if (report.post_deleted) {
    return 'info';
  }
  if (report.post_review_status === 'rejected') {
    return 'danger';
  }
  return 'success';
}

function summarizePostContent(content: string | null) {
  if (!content) {
    return '目标内容已不可见，当前仅保留举报记录。';
  }
  return content.length > 88 ? `${content.slice(0, 88)}...` : content;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.moderation-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.moderation-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.moderation-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.moderation-error {
  margin-bottom: 16px;
}

.moderation-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.report-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.report-cell__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--pet-admin-title);
}

.report-cell__detail {
  color: var(--pet-admin-body);
  line-height: 1.6;
}

.report-cell__meta {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.moderation-actions {
  display: flex;
  gap: 8px;
}

.moderation-dialog-context {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
}

.moderation-dialog-context__title {
  color: var(--pet-admin-title);
  font-size: 14px;
  font-weight: 700;
}

.moderation-dialog-context__meta {
  margin-top: 6px;
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

:deep(.el-table) {
  --el-table-border-color: var(--pet-admin-line);
  --el-table-header-bg-color: #fff8f2;
  --el-table-row-hover-bg-color: #fff6ee;
  --el-table-text-color: var(--pet-admin-body);
  --el-table-header-text-color: var(--pet-admin-title);
  border-radius: 20px;
}

:deep(.el-radio-button__inner) {
  border-radius: 14px;
}

:deep(.el-button) {
  border-radius: 14px;
}

:deep(.el-dialog),
:deep(.el-textarea__inner) {
  border-radius: 18px;
}

:deep(.el-dialog__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 960px) {
  .moderation-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .moderation-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .moderation-actions {
    flex-direction: column;
  }
}
</style>
