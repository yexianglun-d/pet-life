<template>
  <section class="page-section message-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">消息模板</p>
      <h1 class="page-section__title">把站内信、短信和 Push 文案维护成清晰模板</h1>
      <p class="page-section__description">
        消息模板只维护可复用文案配置，真实发送仍由通知链路和渠道配置决定。这里支持查询、详情、创建、编辑和启停，并展示配置审计结果。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">模板 {{ templates.length }} 个</span>
        <span class="pet-admin-chip">启用 {{ enabledTemplateCount }} 个</span>
        <span class="pet-admin-chip">站内信 {{ inboxTemplateCount }} 个</span>
        <span class="pet-admin-chip">短信 / Push {{ externalTemplateCount }} 个</span>
      </div>
    </div>

    <div class="summary-grid message-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel message-section">
      <div class="message-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">模板配置</h2>
          <p class="pet-admin-panel__description">
            按关键词、模板编码、渠道和启用状态筛选。保存前会做必填校验，唯一性仍以服务端校验为准。
          </p>
        </div>
        <div class="message-toolbar__actions">
          <el-input v-model="filters.keyword" size="small" class="message-keyword" placeholder="关键词" clearable />
          <el-input
            v-model="filters.templateCode"
            size="small"
            class="message-filter"
            placeholder="模板编码"
            clearable
          />
          <el-select v-model="filters.channelType" size="small" class="message-filter" placeholder="渠道">
            <el-option label="全部渠道" value="all" />
            <el-option v-for="option in channelTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-select v-model="filters.enabled" size="small" class="message-filter" placeholder="启用状态">
            <el-option label="全部状态" value="all" />
            <el-option label="已启用" value="true" />
            <el-option label="已停用" value="false" />
          </el-select>
          <el-button :loading="isLoading || auditLogLoading" @click="loadPage">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="message-error"
        :closable="false"
      />

      <el-table
        :data="templates"
        v-loading="isLoading"
        row-key="template_id"
        empty-text="暂无消息模板"
        class="message-table"
      >
        <el-table-column label="模板" min-width="260">
          <template #default="{ row }">
            <div class="message-cell">
              <div class="message-cell__title">
                <span>{{ row.template_code }}</span>
                <el-tag size="small" :type="channelTagType(row.channel_type)">
                  {{ channelTypeLabel(row.channel_type) }}
                </el-tag>
              </div>
              <div class="message-cell__meta">模板 ID：{{ row.template_id }}</div>
              <div class="message-cell__meta">标题：{{ row.title_template || '未配置标题' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="内容模板" min-width="360">
          <template #default="{ row }">
            <div class="message-cell">
              <div class="message-cell__detail">{{ summarizeText(row.content_template, 120) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updated_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="message-actions">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.enabled ? 'warning' : 'success'"
                :loading="processingTemplateId === row.template_id"
                @click="toggleTemplateStatus(row)"
              >
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel message-section">
      <div class="message-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">消息模板审计</h2>
          <p class="pet-admin-panel__description">
            查询服务端记录的模板创建、编辑和启停动作，便于运营排查配置变化。
          </p>
        </div>
        <div class="message-toolbar__actions">
          <el-select v-model="auditLogFilters.action" size="small" class="message-audit-filter" placeholder="动作">
            <el-option label="全部动作" value="all" />
            <el-option label="创建模板" value="message_template_create" />
            <el-option label="编辑模板" value="message_template_update" />
            <el-option label="启停模板" value="message_template_status_update" />
          </el-select>
          <el-input
            v-model="auditLogFilters.operatorId"
            size="small"
            class="message-audit-filter"
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
        class="message-error"
        :closable="false"
      />

      <el-table
        :data="auditLogs"
        v-loading="auditLogLoading"
        row-key="audit_log_id"
        empty-text="暂无消息模板审计记录"
        class="message-table"
      >
        <el-table-column label="操作" min-width="240">
          <template #default="{ row }">
            <div class="message-cell">
              <div class="message-cell__title">
                <span>{{ auditActionLabel(row.action) }}</span>
                <el-tag size="small" type="info">{{ row.target_type }}</el-tag>
              </div>
              <div class="message-cell__meta">目标 ID：{{ row.target_id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }">
            <div class="message-cell">
              <div class="message-cell__title">{{ row.operator_id }}</div>
              <div class="message-cell__meta">{{ row.ip_address || '未知 IP' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="320">
          <template #default="{ row }">
            <pre class="message-audit-detail">{{ formatAuditDetail(row.detail_json) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="消息模板详情" size="540px">
      <div v-loading="detailLoading" class="message-detail">
        <template v-if="activeTemplate">
          <section class="message-detail__section">
            <h3>{{ activeTemplate.template_code }}</h3>
            <div class="message-detail__tags">
              <el-tag :type="channelTagType(activeTemplate.channel_type)">
                {{ channelTypeLabel(activeTemplate.channel_type) }}
              </el-tag>
              <el-tag :type="activeTemplate.enabled ? 'success' : 'info'">
                {{ activeTemplate.enabled ? '已启用' : '已停用' }}
              </el-tag>
            </div>
            <dl class="message-detail-list">
              <div>
                <dt>模板 ID</dt>
                <dd>{{ activeTemplate.template_id }}</dd>
              </div>
              <div>
                <dt>模板编码</dt>
                <dd>{{ activeTemplate.template_code }}</dd>
              </div>
              <div>
                <dt>渠道</dt>
                <dd>{{ channelTypeLabel(activeTemplate.channel_type) }}</dd>
              </div>
              <div>
                <dt>标题模板</dt>
                <dd>{{ activeTemplate.title_template || '-' }}</dd>
              </div>
              <div>
                <dt>内容模板</dt>
                <dd class="message-detail-list__content">{{ activeTemplate.content_template }}</dd>
              </div>
              <div>
                <dt>创建 / 更新</dt>
                <dd>{{ formatDateTime(activeTemplate.created_at) }} / {{ formatDateTime(activeTemplate.updated_at) }}</dd>
              </div>
            </dl>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="templateDialogVisible" :title="editingTemplateId ? '编辑消息模板' : '新增消息模板'" width="580px">
      <div class="message-form">
        <div class="message-form__grid">
          <label class="message-form__item">
            <span>模板编码</span>
            <el-input v-model="templateForm.templateCode" maxlength="64" show-word-limit placeholder="例如：user_welcome" />
          </label>
          <label class="message-form__item">
            <span>渠道</span>
            <el-select v-model="templateForm.channelType" placeholder="选择渠道">
              <el-option v-for="option in channelTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </label>
        </div>
        <label class="message-form__item">
          <span>标题模板</span>
          <el-input v-model="templateForm.titleTemplate" maxlength="100" show-word-limit placeholder="站内信和 Push 可配置标题，短信可留空" />
        </label>
        <label class="message-form__item">
          <span>内容模板</span>
          <el-input
            v-model="templateForm.contentTemplate"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="填写发送给用户的消息正文模板"
          />
        </label>
        <label class="message-form__item">
          <span>启用状态</span>
          <el-switch v-model="templateForm.enabled" active-text="启用" inactive-text="停用" />
        </label>
      </div>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSubmitting" @click="submitTemplateForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  createAdminMessageTemplate,
  getAdminMessageTemplate,
  listAdminMessageTemplates,
  listNotificationAuditLogs,
  updateAdminMessageTemplate,
  updateAdminMessageTemplateStatus,
  type MessageTemplateSnapshot,
  type NotificationAuditLogSnapshot,
  type NotificationChannelType,
  type NotificationChannelTypeFilter,
  type NotificationEnabledFilter,
  type UpsertMessageTemplatePayload
} from '@/shared/api/adminNotificationConfigApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

interface MessageTemplateForm {
  templateCode: string;
  channelType: NotificationChannelType;
  titleTemplate: string;
  contentTemplate: string;
  enabled: boolean;
}

const channelTypeOptions: Array<{ label: string; value: NotificationChannelType }> = [
  { label: '站内信', value: 'inbox' },
  { label: '短信', value: 'sms' },
  { label: 'Push', value: 'push' }
];

const templates = ref<MessageTemplateSnapshot[]>([]);
const auditLogs = ref<NotificationAuditLogSnapshot[]>([]);
const isLoading = ref(false);
const auditLogLoading = ref(false);
const detailLoading = ref(false);
const templateSubmitting = ref(false);
const errorMessage = ref('');
const auditLogErrorMessage = ref('');
const detailDrawerVisible = ref(false);
const templateDialogVisible = ref(false);
const activeTemplate = ref<MessageTemplateSnapshot | null>(null);
const editingTemplateId = ref<string | null>(null);
const processingTemplateId = ref<string | null>(null);

const filters = reactive<{
  keyword: string;
  templateCode: string;
  channelType: NotificationChannelTypeFilter;
  enabled: NotificationEnabledFilter;
}>({
  keyword: '',
  templateCode: '',
  channelType: 'all',
  enabled: 'all'
});

const auditLogFilters = reactive<{
  operatorId: string;
  action: string;
}>({
  operatorId: '',
  action: 'all'
});

const templateForm = reactive<MessageTemplateForm>(createDefaultTemplateForm());

const enabledTemplateCount = computed(() => templates.value.filter((template) => template.enabled).length);
const disabledTemplateCount = computed(() => templates.value.filter((template) => !template.enabled).length);
const inboxTemplateCount = computed(
  () => templates.value.filter((template) => template.channel_type === 'inbox').length
);
const externalTemplateCount = computed(
  () => templates.value.filter((template) => template.channel_type === 'sms' || template.channel_type === 'push').length
);

const summaryCards = computed(() => [
  {
    title: '模板总数',
    description: '当前筛选条件下返回的消息模板。',
    value: `${templates.value.length} 个`
  },
  {
    title: '启用模板',
    description: '可被通知链路读取的模板配置。',
    value: `${enabledTemplateCount.value} 个`
  },
  {
    title: '外部渠道模板',
    description: '短信与 Push 文案模板，仅代表文案配置。',
    value: `${externalTemplateCount.value} 个`
  },
  {
    title: '停用模板',
    description: '保留配置但暂不参与发送选择。',
    value: `${disabledTemplateCount.value} 个`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  await Promise.all([loadTemplates(), loadAuditLogs()]);
}

async function loadTemplates() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    templates.value = await listAdminMessageTemplates({
      keyword: filters.keyword,
      templateCode: filters.templateCode,
      channelType: filters.channelType,
      enabled: filters.enabled
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '消息模板加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function loadAuditLogs() {
  auditLogLoading.value = true;
  auditLogErrorMessage.value = '';
  try {
    auditLogs.value = await listNotificationAuditLogs({
      targetType: 'message_template',
      operatorId: auditLogFilters.operatorId,
      action: auditLogFilters.action
    });
  } catch (error) {
    auditLogErrorMessage.value = error instanceof Error ? error.message : '消息模板审计加载失败';
  } finally {
    auditLogLoading.value = false;
  }
}

async function openDetail(template: MessageTemplateSnapshot) {
  detailDrawerVisible.value = true;
  activeTemplate.value = template;
  detailLoading.value = true;
  try {
    activeTemplate.value = await getAdminMessageTemplate(template.template_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '消息模板详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openCreateDialog() {
  editingTemplateId.value = null;
  Object.assign(templateForm, createDefaultTemplateForm());
  templateDialogVisible.value = true;
}

async function openEditDialog(template: MessageTemplateSnapshot) {
  editingTemplateId.value = template.template_id;
  templateSubmitting.value = true;
  try {
    const latestTemplate = await getAdminMessageTemplate(template.template_id);
    Object.assign(templateForm, toTemplateForm(latestTemplate));
    templateDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '消息模板详情加载失败');
  } finally {
    templateSubmitting.value = false;
  }
}

async function submitTemplateForm() {
  if (!validateTemplateForm()) {
    return;
  }

  templateSubmitting.value = true;
  try {
    const payload = toUpsertPayload();
    const savedTemplate = editingTemplateId.value
      ? await updateAdminMessageTemplate(editingTemplateId.value, payload)
      : await createAdminMessageTemplate(payload);
    if (activeTemplate.value?.template_id === savedTemplate.template_id) {
      activeTemplate.value = savedTemplate;
    }
    templateDialogVisible.value = false;
    ElMessage.success(editingTemplateId.value ? '消息模板已更新' : '消息模板已创建');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '消息模板保存失败');
  } finally {
    templateSubmitting.value = false;
  }
}

async function toggleTemplateStatus(template: MessageTemplateSnapshot) {
  const nextEnabled = !template.enabled;
  const actionLabel = nextEnabled ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}消息模板「${template.template_code}」？操作会写入后台审计日志。`,
      `${actionLabel}消息模板`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        type: nextEnabled ? 'success' : 'warning'
      }
    );
    processingTemplateId.value = template.template_id;
    const updatedTemplate = await updateAdminMessageTemplateStatus(template.template_id, nextEnabled);
    if (activeTemplate.value?.template_id === updatedTemplate.template_id) {
      activeTemplate.value = updatedTemplate;
    }
    ElMessage.success(updatedTemplate.enabled ? '消息模板已启用' : '消息模板已停用');
    await loadPage();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : `${actionLabel}失败`);
    }
  } finally {
    processingTemplateId.value = null;
  }
}

function createDefaultTemplateForm(): MessageTemplateForm {
  return {
    templateCode: '',
    channelType: 'inbox',
    titleTemplate: '',
    contentTemplate: '',
    enabled: true
  };
}

function toTemplateForm(template: MessageTemplateSnapshot): MessageTemplateForm {
  return {
    templateCode: template.template_code,
    channelType: template.channel_type,
    titleTemplate: template.title_template ?? '',
    contentTemplate: template.content_template,
    enabled: template.enabled
  };
}

function validateTemplateForm() {
  if (!templateForm.templateCode.trim()) {
    ElMessage.warning('请填写模板编码');
    return false;
  }
  if (!templateForm.contentTemplate.trim()) {
    ElMessage.warning('请填写内容模板');
    return false;
  }
  return true;
}

function toUpsertPayload(): UpsertMessageTemplatePayload {
  return {
    template_code: templateForm.templateCode.trim(),
    channel_type: templateForm.channelType,
    title_template: normalizeNullableText(templateForm.titleTemplate),
    content_template: templateForm.contentTemplate.trim(),
    enabled: templateForm.enabled
  };
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function channelTypeLabel(channelType: NotificationChannelType) {
  const labelMap: Record<NotificationChannelType, string> = {
    inbox: '站内信',
    sms: '短信',
    push: 'Push'
  };
  return labelMap[channelType];
}

function channelTagType(channelType: NotificationChannelType): ElementTagType {
  if (channelType === 'inbox') {
    return 'success';
  }
  if (channelType === 'sms') {
    return 'warning';
  }
  return 'primary';
}

function auditActionLabel(action: string) {
  const labelMap: Record<string, string> = {
    message_template_create: '创建模板',
    message_template_update: '编辑模板',
    message_template_status_update: '启停模板'
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

function formatAuditDetail(detailJson: string) {
  try {
    return JSON.stringify(JSON.parse(detailJson), null, 2);
  } catch {
    return detailJson || '{}';
  }
}
</script>

<style scoped>
.message-summary,
.message-section {
  margin-top: 24px;
}

.message-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.message-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.message-filter {
  width: 132px;
}

.message-keyword {
  width: 180px;
}

.message-audit-filter {
  width: 160px;
}

.message-error {
  margin-bottom: 16px;
}

.message-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.message-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.message-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.message-cell__meta,
.message-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.message-cell__detail {
  color: var(--pet-admin-body);
}

.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.message-audit-detail {
  max-height: 132px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--pet-admin-body);
  font-size: 12px;
  line-height: 1.6;
}

.message-detail {
  min-height: 320px;
}

.message-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.message-detail__section:first-child {
  padding-top: 0;
}

.message-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.message-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.message-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.message-detail-list div {
  display: grid;
  gap: 5px;
}

.message-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.message-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.message-detail-list__content {
  white-space: pre-wrap;
}

.message-form {
  display: grid;
  gap: 16px;
}

.message-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.message-form__item {
  display: grid;
  gap: 8px;
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.message-form__item :deep(.el-select) {
  width: 100%;
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
:deep(.el-dialog),
:deep(.el-drawer),
:deep(.el-textarea__inner) {
  border-radius: 14px;
}

:deep(.el-dialog__title),
:deep(.el-drawer__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 1120px) {
  .message-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .message-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .message-filter,
  .message-keyword,
  .message-audit-filter {
    width: 100%;
  }

  .message-form__grid {
    grid-template-columns: 1fr;
  }

  .message-toolbar__actions,
  .message-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
