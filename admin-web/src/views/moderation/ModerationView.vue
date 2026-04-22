<template>
  <section class="page-section">
    <h1 class="page-section__title">审核中心</h1>
    <p class="page-section__description">
      当前先承接社区举报处理主链路，支持查看真实举报记录并完成处理动作。
    </p>

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
    </div>

    <div class="moderation-toolbar">
      <el-radio-group v-model="statusFilter" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="pending">待处理</el-radio-button>
        <el-radio-button label="processed">确认违规</el-radio-button>
        <el-radio-button label="rejected">驳回举报</el-radio-button>
      </el-radio-group>
      <el-button :loading="isLoading" @click="loadReports">刷新列表</el-button>
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
              @click="handleProcess(row, 'confirm_violation')"
            >
              确认违规
            </el-button>
            <el-button
              size="small"
              :disabled="row.status !== 'pending' || processingReportId === row.report_id"
              @click="handleProcess(row, 'dismiss_report')"
            >
              驳回举报
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, ref } from 'vue';

const reports = ref<ModerationReportSnapshot[]>([]);
const isLoading = ref(false);
const processingReportId = ref<string | null>(null);
const errorMessage = ref('');
const statusFilter = ref<ModerationReportListFilter>('pending');

const visibleReports = computed(() => {
  if (statusFilter.value === 'all') {
    return reports.value;
  }
  return reports.value.filter((report) => report.status === statusFilter.value);
});

const pendingCount = computed(() => reports.value.filter((report) => report.status === 'pending').length);
const processedCount = computed(() => reports.value.filter((report) => report.status === 'processed').length);
const rejectedCount = computed(() => reports.value.filter((report) => report.status === 'rejected').length);

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

async function handleProcess(report: ModerationReportSnapshot, action: ModerationProcessAction) {
  const actionLabel = action === 'confirm_violation' ? '确认违规' : '驳回举报';
  try {
    await ElMessageBox.confirm(
      `确认要执行“${actionLabel}”吗？`,
      '处理举报',
      {
        type: action === 'confirm_violation' ? 'warning' : 'info',
        confirmButtonText: actionLabel,
        cancelButtonText: '取消'
      }
    );
  } catch {
    return;
  }

  processingReportId.value = report.report_id;
  try {
    const updatedReport = await processModerationReport(report.report_id, action);
    reports.value = reports.value.map((item) =>
      item.report_id === updatedReport.report_id ? updatedReport : item
    );
    ElMessage.success(`${actionLabel}已完成`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '举报处理失败');
  } finally {
    processingReportId.value = null;
  }
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
  margin-bottom: 20px;
}

.moderation-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.moderation-error {
  margin-bottom: 16px;
}

.moderation-table {
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
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
  color: #0f172a;
}

.report-cell__detail {
  color: #334155;
  line-height: 1.6;
}

.report-cell__meta {
  color: #64748b;
  font-size: 13px;
}

.moderation-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 960px) {
  .moderation-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .moderation-actions {
    flex-direction: column;
  }
}
</style>
