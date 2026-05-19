<template>
  <section class="page-section sms-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">验证码排查</p>
      <h1 class="page-section__title">查看短信发送记录和验证码安全状态</h1>
      <p class="page-section__description">
        当前页面只接入供应商无关的短信验证码安全底座，用于排查服务端受理、频控、错误次数、过期和校验状态；真实短信供应商尚未接入。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">记录 {{ visibleRows.length }} 条</span>
        <span class="pet-admin-chip">已受理 {{ acceptedCount }} 条</span>
        <span class="pet-admin-chip">被拦截 {{ blockedCount }} 条</span>
        <span class="pet-admin-chip">锁定 {{ lockedCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid sms-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <el-alert
      title="安全边界"
      description="后台接口不会返回明文验证码、code_hash 或 salt；页面详情只展示排查字段，不提供查看验证码能力。"
      type="warning"
      show-icon
      class="sms-boundary"
      :closable="false"
    />

    <article class="pet-admin-panel sms-section">
      <div class="sms-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">短信发送与校验记录</h2>
          <p class="pet-admin-panel__description">
            按手机号、场景、发送状态、校验状态、供应商和时间范围筛选。时间范围基于接口真实返回时间字段在页面内收窄。
          </p>
        </div>
        <div class="sms-toolbar__actions">
          <el-input v-model="filters.mobile" size="small" class="sms-filter" placeholder="手机号" clearable />
          <el-select v-model="filters.scene" size="small" class="sms-filter" placeholder="场景">
            <el-option label="全部场景" value="all" />
            <el-option label="登录" value="login" />
          </el-select>
          <el-select v-model="filters.sendStatus" size="small" class="sms-filter" placeholder="发送状态">
            <el-option label="全部发送" value="all" />
            <el-option label="已受理" value="accepted" />
            <el-option label="发送失败" value="failed" />
            <el-option label="频控拦截" value="blocked" />
          </el-select>
          <el-select v-model="filters.verificationStatus" size="small" class="sms-filter" placeholder="校验状态">
            <el-option label="全部校验" value="all" />
            <el-option label="有效" value="active" />
            <el-option label="已校验" value="verified" />
            <el-option label="已过期" value="expired" />
            <el-option label="已锁定" value="locked" />
            <el-option label="发送失败" value="send_failed" />
          </el-select>
          <el-input
            v-model="filters.providerCode"
            size="small"
            class="sms-filter"
            placeholder="provider_code"
            clearable
          />
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            size="small"
            class="sms-time-range"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
          />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="sms-error"
        :closable="false"
      />

      <el-table
        :data="visibleRows"
        v-loading="isLoading"
        row-key="rowId"
        empty-text="暂无短信验证码记录"
        class="sms-table"
      >
        <el-table-column label="手机号 / 场景" min-width="190">
          <template #default="{ row }">
            <div class="sms-cell">
              <div class="sms-cell__title">{{ maskMobile(row.mobile) }}</div>
              <div class="sms-cell__meta">{{ sceneLabel(row.scene) }}</div>
              <div class="sms-cell__meta">记录时间：{{ formatDateTime(row.recordTime) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发送状态" min-width="170">
          <template #default="{ row }">
            <div class="sms-cell">
              <el-tag :type="sendStatusTagType(row)">
                {{ rowSendStatusLabel(row) }}
              </el-tag>
              <div class="sms-cell__meta">供应商：{{ row.sendRecord?.provider_code || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="校验状态" min-width="180">
          <template #default="{ row }">
            <div class="sms-cell">
              <el-tag :type="verificationStatusTagType(row.verificationRecord?.status)">
                {{ rowVerificationStatusLabel(row) }}
              </el-tag>
              <div class="sms-cell__meta">错误次数：{{ attemptLabel(row) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.verificationRecord?.expires_at || null) }}
          </template>
        </el-table-column>

        <el-table-column label="请求来源" min-width="180">
          <template #default="{ row }">
            <div class="sms-cell">
              <div class="sms-cell__meta">IP：{{ rowRequestIp(row) }}</div>
              <div class="sms-cell__meta">UA：{{ summarizeText(rowUserAgent(row), 42) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="失败原因" min-width="240">
          <template #default="{ row }">
            <span class="sms-cell__detail">{{ row.sendRecord?.failure_reason || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="验证码安全排查详情" size="560px">
      <div class="sms-detail">
        <template v-if="activeRow">
          <section class="sms-detail__section">
            <h3>{{ maskMobile(activeRow.mobile) }} · {{ sceneLabel(activeRow.scene) }}</h3>
            <div class="sms-detail__tags">
              <el-tag :type="sendStatusTagType(activeRow)">
                {{ rowSendStatusLabel(activeRow) }}
              </el-tag>
              <el-tag :type="verificationStatusTagType(activeRow.verificationRecord?.status)">
                {{ rowVerificationStatusLabel(activeRow) }}
              </el-tag>
            </div>
            <el-alert
              title="验证码内容不可见"
              description="服务端响应不包含明文验证码、code_hash 或 salt，本详情仅用于排查状态、频控、错误次数和请求来源。"
              type="info"
              show-icon
              :closable="false"
            />
          </section>

          <section class="sms-detail__section">
            <h3>发送记录</h3>
            <dl v-if="activeRow.sendRecord" class="sms-detail-list">
              <div>
                <dt>发送记录 ID</dt>
                <dd>{{ activeRow.sendRecord.send_record_id }}</dd>
              </div>
              <div>
                <dt>关联验证码记录</dt>
                <dd>{{ activeRow.sendRecord.verification_id || '-' }}</dd>
              </div>
              <div>
                <dt>手机号</dt>
                <dd>{{ activeRow.sendRecord.mobile }}</dd>
              </div>
              <div>
                <dt>场景</dt>
                <dd>{{ sceneLabel(activeRow.sendRecord.scene) }}</dd>
              </div>
              <div>
                <dt>供应商编码</dt>
                <dd>{{ activeRow.sendRecord.provider_code }}</dd>
              </div>
              <div>
                <dt>发送状态</dt>
                <dd>{{ sendStatusLabel(activeRow.sendRecord.send_status) }}</dd>
              </div>
              <div>
                <dt>失败原因</dt>
                <dd>{{ activeRow.sendRecord.failure_reason || '-' }}</dd>
              </div>
              <div>
                <dt>请求 IP</dt>
                <dd>{{ activeRow.sendRecord.request_ip || '-' }}</dd>
              </div>
              <div>
                <dt>User-Agent</dt>
                <dd class="sms-detail-list__content">{{ activeRow.sendRecord.user_agent || '-' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(activeRow.sendRecord.created_at) }}</dd>
              </div>
            </dl>
            <div v-else class="sms-empty-inline">暂无发送记录</div>
          </section>

          <section class="sms-detail__section">
            <h3>校验记录</h3>
            <dl v-if="activeRow.verificationRecord" class="sms-detail-list">
              <div>
                <dt>验证码记录 ID</dt>
                <dd>{{ activeRow.verificationRecord.verification_id }}</dd>
              </div>
              <div>
                <dt>手机号</dt>
                <dd>{{ activeRow.verificationRecord.mobile }}</dd>
              </div>
              <div>
                <dt>场景</dt>
                <dd>{{ sceneLabel(activeRow.verificationRecord.scene) }}</dd>
              </div>
              <div>
                <dt>校验状态</dt>
                <dd>{{ verificationStatusLabel(activeRow.verificationRecord.status) }}</dd>
              </div>
              <div>
                <dt>错误次数</dt>
                <dd>{{ activeRow.verificationRecord.attempt_count }} / {{ activeRow.verificationRecord.max_attempt_count }}</dd>
              </div>
              <div>
                <dt>过期时间</dt>
                <dd>{{ formatDateTime(activeRow.verificationRecord.expires_at) }}</dd>
              </div>
              <div>
                <dt>验证通过时间</dt>
                <dd>{{ formatDateTime(activeRow.verificationRecord.verified_at) }}</dd>
              </div>
              <div>
                <dt>请求 IP</dt>
                <dd>{{ activeRow.verificationRecord.request_ip || '-' }}</dd>
              </div>
              <div>
                <dt>User-Agent</dt>
                <dd class="sms-detail-list__content">{{ activeRow.verificationRecord.user_agent || '-' }}</dd>
              </div>
              <div>
                <dt>创建 / 更新</dt>
                <dd>{{ formatDateTime(activeRow.verificationRecord.created_at) }} / {{ formatDateTime(activeRow.verificationRecord.updated_at) }}</dd>
              </div>
            </dl>
            <div v-else class="sms-empty-inline">暂无校验记录</div>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import {
  listAdminSmsSendRecords,
  listAdminSmsVerificationRecords,
  type SmsScene,
  type SmsSceneFilter,
  type SmsSendRecordSnapshot,
  type SmsSendStatus,
  type SmsSendStatusFilter,
  type SmsVerificationRecordSnapshot,
  type SmsVerificationStatus,
  type SmsVerificationStatusFilter
} from '@/shared/api/adminSmsVerificationApi';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

interface SmsSecurityRow {
  rowId: string;
  mobile: string;
  scene: SmsScene;
  sendRecord: SmsSendRecordSnapshot | null;
  verificationRecord: SmsVerificationRecordSnapshot | null;
  recordTime: string;
}

const sendRecords = ref<SmsSendRecordSnapshot[]>([]);
const verificationRecords = ref<SmsVerificationRecordSnapshot[]>([]);
const isLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRow = ref<SmsSecurityRow | null>(null);

const filters = reactive<{
  mobile: string;
  scene: SmsSceneFilter;
  sendStatus: SmsSendStatusFilter;
  verificationStatus: SmsVerificationStatusFilter;
  providerCode: string;
  timeRange: [Date, Date] | null;
}>({
  mobile: '',
  scene: 'all',
  sendStatus: 'all',
  verificationStatus: 'all',
  providerCode: '',
  timeRange: null
});

const visibleRows = computed(() => createSecurityRows().filter(isWithinTimeRange));
const acceptedCount = computed(
  () => visibleRows.value.filter((row) => row.sendRecord?.send_status === 'accepted').length
);
const blockedCount = computed(
  () => visibleRows.value.filter((row) => row.sendRecord?.send_status === 'blocked').length
);
const failedCount = computed(
  () => visibleRows.value.filter((row) => row.sendRecord?.send_status === 'failed').length
);
const lockedCount = computed(
  () => visibleRows.value.filter((row) => row.verificationRecord?.status === 'locked').length
);

const summaryCards = computed(() => [
  {
    title: '记录总数',
    description: '当前筛选条件下的发送与校验排查记录。',
    value: `${visibleRows.value.length} 条`
  },
  {
    title: '发送受理',
    description: '服务端已由供应商适配层受理的发送记录。',
    value: `${acceptedCount.value} 条`
  },
  {
    title: '失败 / 拦截',
    description: '发送失败或被频控拦截的记录。',
    value: `${failedCount.value + blockedCount.value} 条`
  },
  {
    title: '锁定验证码',
    description: '因错误次数达到上限而锁定的验证码记录。',
    value: `${lockedCount.value} 条`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const [nextSendRecords, nextVerificationRecords] = await Promise.all([
      listAdminSmsSendRecords({
        mobile: filters.mobile,
        scene: filters.scene,
        providerCode: filters.providerCode,
        sendStatus: filters.sendStatus
      }),
      listAdminSmsVerificationRecords({
        mobile: filters.mobile,
        scene: filters.scene,
        status: filters.verificationStatus
      })
    ]);
    sendRecords.value = nextSendRecords;
    verificationRecords.value = nextVerificationRecords;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '短信验证码记录加载失败';
  } finally {
    isLoading.value = false;
  }
}

function createSecurityRows() {
  const verificationMap = new Map(
    verificationRecords.value.map((record) => [record.verification_id, record])
  );
  const linkedVerificationIds = new Set<string>();
  const rows = sendRecords.value
    .map<SmsSecurityRow>((record) => {
      const verificationRecord = record.verification_id ? verificationMap.get(record.verification_id) ?? null : null;
      if (record.verification_id) {
        linkedVerificationIds.add(record.verification_id);
      }
      return {
        rowId: `send-${record.send_record_id}`,
        mobile: record.mobile,
        scene: record.scene,
        sendRecord: record,
        verificationRecord,
        recordTime: record.created_at
      };
    })
    .filter((row) => filters.verificationStatus === 'all' || Boolean(row.verificationRecord));

  if (!hasSendSpecificFilter()) {
    const verificationOnlyRows = verificationRecords.value
      .filter((record) => !linkedVerificationIds.has(record.verification_id))
      .map<SmsSecurityRow>((record) => ({
        rowId: `verification-${record.verification_id}`,
        mobile: record.mobile,
        scene: record.scene,
        sendRecord: null,
        verificationRecord: record,
        recordTime: record.created_at
      }));
    rows.push(...verificationOnlyRows);
  }

  return rows.sort((left, right) => new Date(right.recordTime).getTime() - new Date(left.recordTime).getTime());
}

function hasSendSpecificFilter() {
  return Boolean(filters.providerCode.trim()) || filters.sendStatus !== 'all';
}

function isWithinTimeRange(row: SmsSecurityRow) {
  if (!filters.timeRange) {
    return true;
  }
  const [startTime, endTime] = filters.timeRange;
  const rowTime = new Date(row.recordTime).getTime();
  return rowTime >= startTime.getTime() && rowTime <= endTime.getTime();
}

function openDetail(row: SmsSecurityRow) {
  activeRow.value = row;
  detailDrawerVisible.value = true;
}

function maskMobile(mobile: string) {
  if (/^\d{11}$/.test(mobile)) {
    return `${mobile.slice(0, 3)}****${mobile.slice(7)}`;
  }
  if (mobile.length <= 4) {
    return '****';
  }
  return `${mobile.slice(0, 3)}****${mobile.slice(-2)}`;
}

function sceneLabel(scene: SmsScene) {
  const labelMap: Record<SmsScene, string> = {
    login: '短信登录'
  };
  return labelMap[scene];
}

function sendStatusLabel(status: SmsSendStatus) {
  const labelMap: Record<SmsSendStatus, string> = {
    accepted: '已受理',
    failed: '发送失败',
    blocked: '频控拦截'
  };
  return labelMap[status];
}

function rowSendStatusLabel(row: SmsSecurityRow) {
  return row.sendRecord ? sendStatusLabel(row.sendRecord.send_status) : '无发送记录';
}

function sendStatusTagType(row: SmsSecurityRow): ElementTagType {
  if (!row.sendRecord) {
    return 'info';
  }
  if (row.sendRecord.send_status === 'accepted') {
    return 'success';
  }
  if (row.sendRecord.send_status === 'blocked') {
    return 'warning';
  }
  return 'danger';
}

function verificationStatusLabel(status: SmsVerificationStatus) {
  const labelMap: Record<SmsVerificationStatus, string> = {
    active: '有效',
    verified: '已校验',
    expired: '已过期',
    locked: '已锁定',
    send_failed: '发送失败'
  };
  return labelMap[status];
}

function rowVerificationStatusLabel(row: SmsSecurityRow) {
  return row.verificationRecord ? verificationStatusLabel(row.verificationRecord.status) : '无校验记录';
}

function verificationStatusTagType(status: SmsVerificationStatus | undefined): ElementTagType {
  if (!status) {
    return 'info';
  }
  if (status === 'verified') {
    return 'success';
  }
  if (status === 'active') {
    return 'primary';
  }
  if (status === 'locked' || status === 'send_failed') {
    return 'danger';
  }
  return 'warning';
}

function attemptLabel(row: SmsSecurityRow) {
  if (!row.verificationRecord) {
    return '-';
  }
  return `${row.verificationRecord.attempt_count} / ${row.verificationRecord.max_attempt_count}`;
}

function rowRequestIp(row: SmsSecurityRow) {
  return row.sendRecord?.request_ip || row.verificationRecord?.request_ip || '-';
}

function rowUserAgent(row: SmsSecurityRow) {
  return row.sendRecord?.user_agent || row.verificationRecord?.user_agent || '-';
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
.sms-summary,
.sms-section,
.sms-boundary {
  margin-top: 24px;
}

.sms-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.sms-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.sms-filter {
  width: 132px;
}

.sms-time-range {
  width: 330px;
}

.sms-error {
  margin-bottom: 16px;
}

.sms-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.sms-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.sms-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.sms-cell__meta,
.sms-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.sms-cell__detail {
  color: var(--pet-admin-body);
}

.sms-detail {
  min-height: 320px;
}

.sms-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.sms-detail__section:first-child {
  padding-top: 0;
}

.sms-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.sms-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.sms-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.sms-detail-list div {
  display: grid;
  gap: 5px;
}

.sms-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.sms-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.sms-detail-list__content {
  white-space: pre-wrap;
  word-break: break-word;
}

.sms-empty-inline {
  padding: 16px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
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

@media (max-width: 1120px) {
  .sms-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .sms-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .sms-filter,
  .sms-time-range {
    width: 100%;
  }

  .sms-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
