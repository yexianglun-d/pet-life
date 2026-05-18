<template>
  <section class="page-section user-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">用户治理</p>
      <h1 class="page-section__title">查询用户资料、通知设置和当前陪伴上下文</h1>
      <p class="page-section__description">
        用户管理页只做真实运营查询和详情查看，不提前扩展封禁、恢复等写治理动作。当前重点是看清账号资料、城市、通知偏好、主要家庭和当前宠物。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">用户 {{ records.length }} 个</span>
        <span class="pet-admin-chip">开启通知 {{ notificationEnabledCount }} 个</span>
        <span class="pet-admin-chip">隐私模式 {{ privateUserCount }} 个</span>
        <span class="pet-admin-chip">已有当前宠物 {{ currentPetCount }} 个</span>
      </div>
    </div>

    <div class="summary-grid user-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel user-section">
      <div class="user-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">用户查询</h2>
          <p class="pet-admin-panel__description">
            按关键词、手机号、昵称、城市、通知开关或隐私级别筛选。列表和详情均来自管理端用户接口。
          </p>
        </div>
        <div class="user-toolbar__actions">
          <el-input v-model="filters.keyword" size="small" class="user-keyword" placeholder="手机号 / 昵称 / 城市 / 家庭 / 宠物" clearable />
          <el-input v-model="filters.mobile" size="small" class="user-filter" placeholder="手机号" clearable />
          <el-input v-model="filters.nickname" size="small" class="user-filter" placeholder="昵称" clearable />
          <el-input v-model="filters.cityCode" size="small" class="user-filter" placeholder="城市编码" clearable />
          <el-select v-model="filters.notificationEnabled" size="small" class="user-filter" placeholder="通知开关">
            <el-option label="全部通知" value="all" />
            <el-option label="已开启" value="true" />
            <el-option label="已关闭" value="false" />
          </el-select>
          <el-select v-model="filters.privacyLevel" size="small" class="user-filter" placeholder="隐私级别">
            <el-option label="全部隐私" value="all" />
            <el-option label="普通" value="normal" />
            <el-option label="隐私" value="private" />
          </el-select>
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="user-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="user_id"
        empty-text="暂无用户"
        class="user-table"
      >
        <el-table-column label="用户" min-width="260">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell__title">
                <span>{{ row.nickname || '未设置昵称' }}</span>
                <el-tag size="small" :type="userStatusTagType(row.status)">
                  {{ userStatusLabel(row.status) }}
                </el-tag>
              </div>
              <div class="user-cell__meta">{{ row.mobile }}</div>
              <div class="user-cell__meta">{{ row.city_name || '未知城市' }} · {{ row.city_code || '无城市编码' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="设置" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell__title">
                <el-tag size="small" :type="row.settings.notification_enabled ? 'success' : 'info'">
                  {{ row.settings.notification_enabled ? '通知开启' : '通知关闭' }}
                </el-tag>
                <el-tag size="small" :type="privacyTagType(row.settings.privacy_level)">
                  {{ privacyLabel(row.settings.privacy_level) }}
                </el-tag>
              </div>
              <div class="user-cell__meta">当前宠物 ID：{{ row.settings.current_pet_id || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="主要家庭" min-width="230">
          <template #default="{ row }">
            <div class="user-cell">
              <template v-if="row.primary_family">
                <div class="user-cell__title">{{ row.primary_family.family_name }}</div>
                <div class="user-cell__meta">
                  {{ familyRoleLabel(row.primary_family.role) }} · 成员 {{ row.primary_family.member_count }} 人
                </div>
              </template>
              <span v-else class="user-cell__meta">暂无主要家庭</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前宠物" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <template v-if="row.current_pet">
                <div class="user-cell__title">{{ row.current_pet.pet_name }}</div>
                <div class="user-cell__meta">
                  {{ petTypeLabel(row.current_pet.pet_type) }} · {{ row.current_pet.family_name || '暂无家庭' }}
                </div>
              </template>
              <span v-else class="user-cell__meta">暂无当前宠物</span>
              <div class="user-cell__meta">可见宠物 {{ row.pet_count }} 只</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell__meta">创建：{{ formatDateTime(row.created_at) }}</div>
              <div class="user-cell__meta">登录：{{ row.last_login_at ? formatDateTime(row.last_login_at) : '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="178" fixed="right">
          <template #default="{ row }">
            <div class="user-action-cell">
              <el-button size="small" @click="openDetail(row)">查看详情</el-button>
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="handleUserStatusChange(row)"
              >
                {{ row.status === 1 ? '封禁' : '恢复' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="用户详情" size="520px">
      <div v-loading="detailLoading" class="user-detail">
        <template v-if="activeRecord">
          <section class="user-detail__section">
            <h3>{{ activeRecord.nickname || '未设置昵称' }}</h3>
            <div class="user-detail__tags">
              <el-tag :type="userStatusTagType(activeRecord.status)">
                {{ userStatusLabel(activeRecord.status) }}
              </el-tag>
              <el-tag :type="privacyTagType(activeRecord.settings.privacy_level)">
                {{ privacyLabel(activeRecord.settings.privacy_level) }}
              </el-tag>
            </div>
            <dl class="user-detail-list">
              <div>
                <dt>用户 ID</dt>
                <dd>{{ activeRecord.user_id }}</dd>
              </div>
              <div>
                <dt>手机号</dt>
                <dd>{{ activeRecord.mobile }}</dd>
              </div>
              <div>
                <dt>城市</dt>
                <dd>{{ activeRecord.city_name || '-' }} · {{ activeRecord.city_code || '-' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(activeRecord.created_at) }}</dd>
              </div>
              <div>
                <dt>最近登录</dt>
                <dd>{{ activeRecord.last_login_at ? formatDateTime(activeRecord.last_login_at) : '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="user-detail__section">
            <h3>通知与隐私</h3>
            <dl class="user-detail-list">
              <div>
                <dt>通知总开关</dt>
                <dd>{{ activeRecord.settings.notification_enabled ? '开启' : '关闭' }}</dd>
              </div>
              <div>
                <dt>隐私级别</dt>
                <dd>{{ privacyLabel(activeRecord.settings.privacy_level) }}</dd>
              </div>
              <div>
                <dt>设置中的当前宠物 ID</dt>
                <dd>{{ activeRecord.settings.current_pet_id || '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="user-detail__section">
            <h3>家庭与宠物</h3>
            <dl class="user-detail-list">
              <div>
                <dt>主要家庭</dt>
                <dd>
                  <template v-if="activeRecord.primary_family">
                    {{ activeRecord.primary_family.family_name }} ·
                    {{ familyRoleLabel(activeRecord.primary_family.role) }} ·
                    {{ activeRecord.primary_family.member_count }} 人
                  </template>
                  <template v-else>-</template>
                </dd>
              </div>
              <div>
                <dt>当前宠物</dt>
                <dd>
                  <template v-if="activeRecord.current_pet">
                    {{ activeRecord.current_pet.pet_name }} · {{ petTypeLabel(activeRecord.current_pet.pet_type) }}
                  </template>
                  <template v-else>-</template>
                </dd>
              </div>
              <div>
                <dt>当前宠物家庭</dt>
                <dd>{{ activeRecord.current_pet?.family_name || '-' }}</dd>
              </div>
              <div>
                <dt>可见宠物数</dt>
                <dd>{{ activeRecord.pet_count }} 只</dd>
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
  getAdminUser,
  listAdminUsers,
  updateAdminUserStatus,
  type AdminBooleanFilter,
  type AdminUserSnapshot,
  type FamilyRole,
  type UserPrivacyLevel,
  type UserPrivacyLevelFilter
} from '@/shared/api/adminGovernanceApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const records = ref<AdminUserSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminUserSnapshot | null>(null);

const filters = reactive<{
  keyword: string;
  mobile: string;
  nickname: string;
  cityCode: string;
  notificationEnabled: AdminBooleanFilter;
  privacyLevel: UserPrivacyLevelFilter;
}>({
  keyword: '',
  mobile: '',
  nickname: '',
  cityCode: '',
  notificationEnabled: 'all',
  privacyLevel: 'all'
});

const notificationEnabledCount = computed(
  () => records.value.filter((record) => record.settings.notification_enabled).length
);
const privateUserCount = computed(
  () => records.value.filter((record) => record.settings.privacy_level === 'private').length
);
const currentPetCount = computed(
  () => records.value.filter((record) => record.current_pet !== null).length
);
const familyUserCount = computed(
  () => records.value.filter((record) => record.primary_family !== null).length
);

const summaryCards = computed(() => [
  {
    title: '用户总数',
    description: '当前筛选条件下返回的用户。',
    value: `${records.value.length} 个`
  },
  {
    title: '通知开启',
    description: '通知总开关处于开启状态的用户。',
    value: `${notificationEnabledCount.value} 个`
  },
  {
    title: '隐私模式',
    description: '隐私级别为 private 的用户。',
    value: `${privateUserCount.value} 个`
  },
  {
    title: '已有家庭',
    description: '已关联主要家庭的用户。',
    value: `${familyUserCount.value} 个`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminUsers({
      keyword: filters.keyword,
      mobile: filters.mobile,
      nickname: filters.nickname,
      cityCode: filters.cityCode,
      notificationEnabled: filters.notificationEnabled,
      privacyLevel: filters.privacyLevel
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '用户列表加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminUserSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminUser(record.user_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handleUserStatusChange(record: AdminUserSnapshot) {
  const targetStatus = record.status === 1 ? 2 : 1;
  const actionLabel = targetStatus === 2 ? '封禁' : '恢复';
  try {
    const result = await ElMessageBox.prompt(
      `请输入${actionLabel}原因，后续会写入后台审计日志。`,
      `${actionLabel}用户`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        inputPlaceholder: '例如：举报核实 / 误封恢复'
      }
    );
    await updateAdminUserStatus(record.user_id, {
      status: targetStatus,
      reason: result.value
    });
    ElMessage.success(`已${actionLabel}用户`);
    await loadRecords();
    if (activeRecord.value?.user_id === record.user_id) {
      activeRecord.value = await getAdminUser(record.user_id);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : `${actionLabel}失败`);
    }
  }
}

function userStatusLabel(status: number) {
  const labelMap: Record<number, string> = {
    1: '正常',
    2: '停用'
  };
  return labelMap[status] ?? `状态 ${status}`;
}

function userStatusTagType(status: number) {
  return status === 1 ? 'success' : 'warning';
}

function privacyLabel(privacyLevel: UserPrivacyLevel) {
  const labelMap: Record<UserPrivacyLevel, string> = {
    normal: '普通',
    private: '隐私'
  };
  return labelMap[privacyLevel];
}

function privacyTagType(privacyLevel: UserPrivacyLevel) {
  return privacyLevel === 'private' ? 'warning' : 'info';
}

function familyRoleLabel(role: FamilyRole) {
  const labelMap: Record<FamilyRole, string> = {
    owner: '拥有者',
    admin: '管理员',
    member: '成员'
  };
  return labelMap[role];
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗',
    other: '其他'
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
.user-summary,
.user-section {
  margin-top: 24px;
}

.user-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.user-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.user-filter {
  width: 132px;
}

.user-keyword {
  width: 232px;
}

.user-error {
  margin-bottom: 16px;
}

.user-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.user-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.user-cell__meta {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.user-action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-detail {
  min-height: 320px;
}

.user-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.user-detail__section:first-child {
  padding-top: 0;
}

.user-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.user-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.user-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.user-detail-list div {
  display: grid;
  gap: 5px;
}

.user-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.user-detail-list dd {
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
  .user-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .user-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .user-filter,
  .user-keyword {
    width: 100%;
  }

  .user-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
