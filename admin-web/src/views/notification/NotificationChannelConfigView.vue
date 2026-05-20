<template>
  <section class="page-section channel-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">通知发送配置</p>
      <h1 class="page-section__title">把站内信、短信和 Push 渠道状态维护清楚</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">配置 {{ channels.length }} 个</span>
        <span class="pet-admin-chip">启用 {{ enabledChannelCount }} 个</span>
        <span class="pet-admin-chip">Ready {{ readyChannelCount }} 个</span>
        <span class="pet-admin-chip">短信 / Push {{ externalChannelCount }} 个</span>
      </div>
    </div>

    <div class="summary-grid channel-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <el-alert
      title="功能未完成：缺少真实短信和 Push 供应商"
      type="warning"
      show-icon
      class="channel-boundary"
      :closable="false"
    />

    <article class="pet-admin-panel channel-section">
      <div class="channel-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">渠道配置</h2>
        </div>
        <div class="channel-toolbar__actions">
          <el-select v-model="filters.channelType" size="small" class="channel-filter" placeholder="渠道">
            <el-option label="全部渠道" value="all" />
            <el-option v-for="option in channelTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-select v-model="filters.enabled" size="small" class="channel-filter" placeholder="启用状态">
            <el-option label="全部状态" value="all" />
            <el-option label="已启用" value="true" />
            <el-option label="已停用" value="false" />
          </el-select>
          <el-input
            v-model="filters.providerCode"
            size="small"
            class="channel-filter"
            placeholder="供应商编码"
            clearable
          />
          <el-select v-model="filters.configStatus" size="small" class="channel-filter" placeholder="配置状态">
            <el-option label="全部配置" value="all" />
            <el-option v-for="option in configStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-button :loading="isLoading || auditLogLoading" @click="loadPage">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增渠道</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="channel-error"
        :closable="false"
      />

      <el-table
        :data="channels"
        v-loading="isLoading"
        row-key="channel_config_id"
        empty-text="暂无通知渠道配置"
        class="channel-table"
      >
        <el-table-column label="渠道与供应商" min-width="280">
          <template #default="{ row }">
            <div class="channel-cell">
              <div class="channel-cell__title">
                <span>{{ row.provider_name }}</span>
                <el-tag size="small" :type="channelTagType(row.channel_type)">
                  {{ channelTypeLabel(row.channel_type) }}
                </el-tag>
              </div>
              <div class="channel-cell__meta">供应商编码：{{ row.provider_code }}</div>
              <div class="channel-cell__meta">配置 ID：{{ row.channel_config_id }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="180">
          <template #default="{ row }">
            <div class="channel-cell">
              <div class="channel-cell__title">
                <el-tag :type="row.enabled ? 'success' : 'info'">
                  {{ row.enabled ? '已启用' : '已停用' }}
                </el-tag>
                <el-tag :type="configStatusTagType(row.config_status)">
                  {{ configStatusLabel(row.config_status) }}
                </el-tag>
              </div>
              <div class="channel-cell__meta">{{ channelBoundaryLabel(row.channel_type) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="300">
          <template #default="{ row }">
            <div class="channel-cell">
              <div class="channel-cell__detail">{{ row.remark || '暂无备注' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updated_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="channel-actions">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.enabled ? 'warning' : 'success'"
                :loading="processingChannelId === row.channel_config_id"
                @click="toggleChannelStatus(row)"
              >
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel channel-section">
      <div class="channel-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">通知配置审计</h2>
        </div>
        <div class="channel-toolbar__actions">
          <el-select v-model="auditLogFilters.action" size="small" class="channel-audit-filter" placeholder="动作">
            <el-option label="全部动作" value="all" />
            <el-option label="创建渠道" value="notification_channel_create" />
            <el-option label="编辑渠道" value="notification_channel_update" />
            <el-option label="启停渠道" value="notification_channel_status_update" />
          </el-select>
          <el-input
            v-model="auditLogFilters.operatorId"
            size="small"
            class="channel-audit-filter"
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
        class="channel-error"
        :closable="false"
      />

      <el-table
        :data="auditLogs"
        v-loading="auditLogLoading"
        row-key="audit_log_id"
        empty-text="暂无通知渠道审计记录"
        class="channel-table"
      >
        <el-table-column label="操作" min-width="240">
          <template #default="{ row }">
            <div class="channel-cell">
              <div class="channel-cell__title">
                <span>{{ auditActionLabel(row.action) }}</span>
                <el-tag size="small" type="info">{{ row.target_type }}</el-tag>
              </div>
              <div class="channel-cell__meta">目标 ID：{{ row.target_id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }">
            <div class="channel-cell">
              <div class="channel-cell__title">{{ row.operator_id }}</div>
              <div class="channel-cell__meta">{{ row.ip_address || '未知 IP' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="320">
          <template #default="{ row }">
            <pre class="channel-audit-detail">{{ formatAuditDetail(row.detail_json) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="通知渠道详情" size="540px">
      <div v-loading="detailLoading" class="channel-detail">
        <template v-if="activeChannel">
          <section class="channel-detail__section">
            <h3>{{ activeChannel.provider_name }}</h3>
            <div class="channel-detail__tags">
              <el-tag :type="channelTagType(activeChannel.channel_type)">
                {{ channelTypeLabel(activeChannel.channel_type) }}
              </el-tag>
              <el-tag :type="activeChannel.enabled ? 'success' : 'info'">
                {{ activeChannel.enabled ? '已启用' : '已停用' }}
              </el-tag>
              <el-tag :type="configStatusTagType(activeChannel.config_status)">
                {{ configStatusLabel(activeChannel.config_status) }}
              </el-tag>
            </div>
            <dl class="channel-detail-list">
              <div>
                <dt>配置 ID</dt>
                <dd>{{ activeChannel.channel_config_id }}</dd>
              </div>
              <div>
                <dt>渠道</dt>
                <dd>{{ channelTypeLabel(activeChannel.channel_type) }}</dd>
              </div>
              <div>
                <dt>供应商编码</dt>
                <dd>{{ activeChannel.provider_code }}</dd>
              </div>
              <div>
                <dt>供应商名称</dt>
                <dd>{{ activeChannel.provider_name }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ channelBoundaryLabel(activeChannel.channel_type) }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd class="channel-detail-list__content">{{ activeChannel.remark || '-' }}</dd>
              </div>
              <div>
                <dt>创建 / 更新</dt>
                <dd>{{ formatDateTime(activeChannel.created_at) }} / {{ formatDateTime(activeChannel.updated_at) }}</dd>
              </div>
            </dl>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="channelDialogVisible" :title="editingChannelId ? '编辑通知渠道' : '新增通知渠道'" width="580px">
      <div class="channel-form">
        <div class="channel-form__grid">
          <label class="channel-form__item">
            <span>渠道</span>
            <el-select v-model="channelForm.channelType" placeholder="选择渠道">
              <el-option v-for="option in channelTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </label>
          <label class="channel-form__item">
            <span>配置状态</span>
            <el-select v-model="channelForm.configStatus" placeholder="选择配置状态">
              <el-option v-for="option in configStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </label>
        </div>
        <div class="channel-form__grid">
          <label class="channel-form__item">
            <span>供应商编码</span>
            <el-input v-model="channelForm.providerCode" maxlength="64" show-word-limit placeholder="例如：aliyun" />
          </label>
          <label class="channel-form__item">
            <span>供应商名称</span>
            <el-input v-model="channelForm.providerName" maxlength="100" show-word-limit placeholder="例如：阿里云短信" />
          </label>
        </div>
        <label class="channel-form__item">
          <span>启用状态</span>
          <el-switch v-model="channelForm.enabled" active-text="启用" inactive-text="停用" />
        </label>
        <label class="channel-form__item">
          <span>备注</span>
          <el-input
            v-model="channelForm.remark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="备注"
          />
        </label>
      </div>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="channelSubmitting" @click="submitChannelForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  createAdminNotificationChannel,
  getAdminNotificationChannel,
  listAdminNotificationChannels,
  listNotificationAuditLogs,
  updateAdminNotificationChannel,
  updateAdminNotificationChannelStatus,
  type NotificationAuditLogSnapshot,
  type NotificationChannelConfigSnapshot,
  type NotificationChannelType,
  type NotificationChannelTypeFilter,
  type NotificationConfigStatus,
  type NotificationConfigStatusFilter,
  type NotificationEnabledFilter,
  type UpsertNotificationChannelPayload
} from '@/shared/api/adminNotificationConfigApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

interface NotificationChannelForm {
  channelType: NotificationChannelType;
  providerCode: string;
  providerName: string;
  enabled: boolean;
  configStatus: NotificationConfigStatus;
  remark: string;
}

const channelTypeOptions: Array<{ label: string; value: NotificationChannelType }> = [
  { label: '站内信', value: 'inbox' },
  { label: '短信', value: 'sms' },
  { label: 'Push', value: 'push' }
];

const configStatusOptions: Array<{ label: string; value: NotificationConfigStatus }> = [
  { label: '草稿', value: 'draft' },
  { label: 'Ready', value: 'ready' },
  { label: '已禁用', value: 'disabled' }
];

const channels = ref<NotificationChannelConfigSnapshot[]>([]);
const auditLogs = ref<NotificationAuditLogSnapshot[]>([]);
const isLoading = ref(false);
const auditLogLoading = ref(false);
const detailLoading = ref(false);
const channelSubmitting = ref(false);
const errorMessage = ref('');
const auditLogErrorMessage = ref('');
const detailDrawerVisible = ref(false);
const channelDialogVisible = ref(false);
const activeChannel = ref<NotificationChannelConfigSnapshot | null>(null);
const editingChannelId = ref<string | null>(null);
const processingChannelId = ref<string | null>(null);

const filters = reactive<{
  channelType: NotificationChannelTypeFilter;
  enabled: NotificationEnabledFilter;
  providerCode: string;
  configStatus: NotificationConfigStatusFilter;
}>({
  channelType: 'all',
  enabled: 'all',
  providerCode: '',
  configStatus: 'all'
});

const auditLogFilters = reactive<{
  operatorId: string;
  action: string;
}>({
  operatorId: '',
  action: 'all'
});

const channelForm = reactive<NotificationChannelForm>(createDefaultChannelForm());

const enabledChannelCount = computed(() => channels.value.filter((channel) => channel.enabled).length);
const readyChannelCount = computed(() => channels.value.filter((channel) => channel.config_status === 'ready').length);
const draftChannelCount = computed(() => channels.value.filter((channel) => channel.config_status === 'draft').length);
const disabledConfigCount = computed(
  () => channels.value.filter((channel) => channel.config_status === 'disabled').length
);
const externalChannelCount = computed(
  () => channels.value.filter((channel) => channel.channel_type === 'sms' || channel.channel_type === 'push').length
);

const summaryCards = computed(() => [
  {
    title: '配置总数',
    description: '当前筛选条件下返回的渠道配置。',
    value: `${channels.value.length} 个`
  },
  {
    title: 'Ready 配置',
    description: '具备启用条件的配置状态。',
    value: `${readyChannelCount.value} 个`
  },
  {
    title: '草稿配置',
    description: '仍需补齐或确认的渠道配置。',
    value: `${draftChannelCount.value} 个`
  },
  {
    title: '禁用配置',
    description: '明确停用的渠道配置。',
    value: `${disabledConfigCount.value} 个`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  await Promise.all([loadChannels(), loadAuditLogs()]);
}

async function loadChannels() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    channels.value = await listAdminNotificationChannels({
      channelType: filters.channelType,
      enabled: filters.enabled,
      providerCode: filters.providerCode,
      configStatus: filters.configStatus
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '通知渠道配置加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function loadAuditLogs() {
  auditLogLoading.value = true;
  auditLogErrorMessage.value = '';
  try {
    auditLogs.value = await listNotificationAuditLogs({
      targetType: 'notification_channel',
      operatorId: auditLogFilters.operatorId,
      action: auditLogFilters.action
    });
  } catch (error) {
    auditLogErrorMessage.value = error instanceof Error ? error.message : '通知配置审计加载失败';
  } finally {
    auditLogLoading.value = false;
  }
}

async function openDetail(channel: NotificationChannelConfigSnapshot) {
  detailDrawerVisible.value = true;
  activeChannel.value = channel;
  detailLoading.value = true;
  try {
    activeChannel.value = await getAdminNotificationChannel(channel.channel_config_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知渠道详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openCreateDialog() {
  editingChannelId.value = null;
  Object.assign(channelForm, createDefaultChannelForm());
  channelDialogVisible.value = true;
}

async function openEditDialog(channel: NotificationChannelConfigSnapshot) {
  editingChannelId.value = channel.channel_config_id;
  channelSubmitting.value = true;
  try {
    const latestChannel = await getAdminNotificationChannel(channel.channel_config_id);
    Object.assign(channelForm, toChannelForm(latestChannel));
    channelDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知渠道详情加载失败');
  } finally {
    channelSubmitting.value = false;
  }
}

async function submitChannelForm() {
  if (!validateChannelForm()) {
    return;
  }

  channelSubmitting.value = true;
  try {
    const payload = toUpsertPayload();
    const savedChannel = editingChannelId.value
      ? await updateAdminNotificationChannel(editingChannelId.value, payload)
      : await createAdminNotificationChannel(payload);
    if (activeChannel.value?.channel_config_id === savedChannel.channel_config_id) {
      activeChannel.value = savedChannel;
    }
    channelDialogVisible.value = false;
    ElMessage.success(editingChannelId.value ? '通知渠道已更新' : '通知渠道已创建');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知渠道保存失败');
  } finally {
    channelSubmitting.value = false;
  }
}

async function toggleChannelStatus(channel: NotificationChannelConfigSnapshot) {
  const nextEnabled = !channel.enabled;
  const actionLabel = nextEnabled ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}通知渠道「${channel.provider_name}」？状态变化会写入后台审计日志。`,
      `${actionLabel}通知渠道`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        type: nextEnabled ? 'success' : 'warning'
      }
    );
    processingChannelId.value = channel.channel_config_id;
    const updatedChannel = await updateAdminNotificationChannelStatus(channel.channel_config_id, nextEnabled);
    if (activeChannel.value?.channel_config_id === updatedChannel.channel_config_id) {
      activeChannel.value = updatedChannel;
    }
    ElMessage.success(updatedChannel.enabled ? '通知渠道已启用' : '通知渠道已停用');
    await loadPage();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : `${actionLabel}失败`);
    }
  } finally {
    processingChannelId.value = null;
  }
}

function createDefaultChannelForm(): NotificationChannelForm {
  return {
    channelType: 'inbox',
    providerCode: '',
    providerName: '',
    enabled: false,
    configStatus: 'draft',
    remark: ''
  };
}

function toChannelForm(channel: NotificationChannelConfigSnapshot): NotificationChannelForm {
  return {
    channelType: channel.channel_type,
    providerCode: channel.provider_code,
    providerName: channel.provider_name,
    enabled: channel.enabled,
    configStatus: channel.config_status,
    remark: channel.remark ?? ''
  };
}

function validateChannelForm() {
  if (!channelForm.providerCode.trim()) {
    ElMessage.warning('请填写供应商编码');
    return false;
  }
  if (!channelForm.providerName.trim()) {
    ElMessage.warning('请填写供应商名称');
    return false;
  }
  if (channelForm.enabled && channelForm.configStatus !== 'ready') {
    ElMessage.warning('启用渠道必须保存为 ready 状态');
    return false;
  }
  if (!channelForm.enabled && channelForm.configStatus === 'ready') {
    ElMessage.warning('停用渠道不能保存为 ready 状态');
    return false;
  }
  return true;
}

function toUpsertPayload(): UpsertNotificationChannelPayload {
  return {
    channel_type: channelForm.channelType,
    provider_code: channelForm.providerCode.trim(),
    provider_name: channelForm.providerName.trim(),
    enabled: channelForm.enabled,
    config_status: channelForm.configStatus,
    remark: normalizeNullableText(channelForm.remark)
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

function configStatusLabel(status: NotificationConfigStatus) {
  const labelMap: Record<NotificationConfigStatus, string> = {
    draft: '草稿',
    ready: 'Ready',
    disabled: '已禁用'
  };
  return labelMap[status];
}

function configStatusTagType(status: NotificationConfigStatus): ElementTagType {
  if (status === 'ready') {
    return 'success';
  }
  if (status === 'draft') {
    return 'warning';
  }
  return 'info';
}

function channelBoundaryLabel(channelType: NotificationChannelType) {
  if (channelType === 'inbox') {
    return '-';
  }
  if (channelType === 'sms') {
    return '功能未完成：缺少真实短信供应商。';
  }
  return '功能未完成：缺少真实 Push 供应商。';
}

function auditActionLabel(action: string) {
  const labelMap: Record<string, string> = {
    notification_channel_create: '创建渠道',
    notification_channel_update: '编辑渠道',
    notification_channel_status_update: '启停渠道'
  };
  return labelMap[action] ?? action;
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
.channel-summary,
.channel-section,
.channel-boundary {
  margin-top: 24px;
}

.channel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.channel-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.channel-filter {
  width: 140px;
}

.channel-audit-filter {
  width: 160px;
}

.channel-error {
  margin-bottom: 16px;
}

.channel-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.channel-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.channel-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.channel-cell__meta,
.channel-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.channel-cell__detail {
  color: var(--pet-admin-body);
}

.channel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-audit-detail {
  max-height: 132px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--pet-admin-body);
  font-size: 12px;
  line-height: 1.6;
}

.channel-detail {
  min-height: 320px;
}

.channel-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.channel-detail__section:first-child {
  padding-top: 0;
}

.channel-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.channel-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.channel-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.channel-detail-list div {
  display: grid;
  gap: 5px;
}

.channel-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.channel-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.channel-detail-list__content {
  white-space: pre-wrap;
}

.channel-form {
  display: grid;
  gap: 16px;
}

.channel-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.channel-form__item {
  display: grid;
  gap: 8px;
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.channel-form__item :deep(.el-select) {
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
  .channel-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .channel-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .channel-filter,
  .channel-audit-filter {
    width: 100%;
  }

  .channel-form__grid {
    grid-template-columns: 1fr;
  }

  .channel-toolbar__actions,
  .channel-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
