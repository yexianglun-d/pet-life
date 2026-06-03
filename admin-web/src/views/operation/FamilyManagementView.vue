<template>
  <section class="page-section family-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">家庭共养</p>
      <h1 class="page-section__title">家庭管理</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">家庭 {{ records.length }} 个</span>
        <span class="pet-admin-chip">正常 {{ activeFamilyCount }} 个</span>
        <span class="pet-admin-chip">成员 {{ totalMemberCount }} 人</span>
        <span class="pet-admin-chip">宠物 {{ totalPetCount }} 只</span>
      </div>
    </div>

    <div class="summary-grid family-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel family-section">
      <div class="family-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">家庭查询</h2>
        </div>
        <div class="family-toolbar__actions">
          <el-input v-model="filters.keyword" size="small" class="family-keyword" placeholder="家庭 / 拥有者 / 成员" clearable />
          <el-input v-model="filters.familyName" size="small" class="family-filter" placeholder="家庭名称" clearable />
          <el-input v-model="filters.memberMobile" size="small" class="family-filter" placeholder="成员手机号" clearable />
          <el-select v-model="filters.memberRole" size="small" class="family-filter" placeholder="成员角色">
            <el-option label="全部角色" value="all" />
            <el-option label="拥有者" value="owner" />
            <el-option label="管理员" value="admin" />
            <el-option label="成员" value="member" />
          </el-select>
          <el-select v-model="filters.status" size="small" class="family-filter" placeholder="家庭状态">
            <el-option label="全部状态" value="all" />
            <el-option label="正常" value="1" />
            <el-option label="停用" value="2" />
          </el-select>
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="family-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="family_id"
        empty-text="暂无家庭"
        class="family-table"
      >
        <el-table-column label="家庭" min-width="260">
          <template #default="{ row }">
            <div class="family-cell">
              <div class="family-cell__title">
                <span>{{ row.family_name }}</span>
                <el-tag size="small" :type="familyStatusTagType(row.status)">
                  {{ familyStatusLabel(row.status) }}
                </el-tag>
              </div>
              <div class="family-cell__meta">家庭 ID：{{ row.family_id }}</div>
              <div class="family-cell__meta">创建：{{ formatDateTime(row.created_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="拥有者" min-width="220">
          <template #default="{ row }">
            <div class="family-cell">
              <div class="family-cell__title">{{ row.owner.nickname || '未知用户' }}</div>
              <div class="family-cell__meta">ID：{{ row.owner.user_id }}</div>
              <div v-if="row.owner.mobile" class="family-cell__meta">{{ row.owner.mobile }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="成员关系" min-width="260">
          <template #default="{ row }">
            <div class="family-cell">
              <div class="family-count-line">
                <strong>{{ row.member_count }}</strong>
                <span>位成员</span>
              </div>
              <div class="family-chip-line">
                <el-tag v-for="member in row.members.slice(0, 3)" :key="member.member_id" size="small" type="info">
                  {{ member.nickname || member.mobile || member.user_id }} · {{ familyRoleLabel(member.role) }}
                </el-tag>
                <span v-if="row.members.length === 0" class="family-cell__meta">暂无成员</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="家庭宠物" min-width="240">
          <template #default="{ row }">
            <div class="family-cell">
              <div class="family-count-line">
                <strong>{{ row.pet_count }}</strong>
                <span>只宠物</span>
              </div>
              <div class="family-chip-line">
                <el-tag v-for="pet in row.pets.slice(0, 3)" :key="pet.pet_id" size="small" type="success">
                  {{ pet.pet_name }} · {{ petTypeLabel(pet.pet_type) }}
                </el-tag>
                <span v-if="row.pets.length === 0" class="family-cell__meta">暂无宠物</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updated_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="family-action-cell">
              <el-button size="small" @click="openDetail(row)">查看详情</el-button>
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="handleFamilyStatusChange(row)"
              >
                {{ row.status === 1 ? '停用' : '恢复' }}
              </el-button>
              <el-button size="small" @click="handleOwnerRepair(row)">修复 owner</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="家庭详情" size="560px">
      <div v-loading="detailLoading" class="family-detail">
        <template v-if="activeRecord">
          <section class="family-detail__section">
            <h3>{{ activeRecord.family_name }}</h3>
            <div class="family-detail__tags">
              <el-tag :type="familyStatusTagType(activeRecord.status)">
                {{ familyStatusLabel(activeRecord.status) }}
              </el-tag>
              <el-tag type="info">成员 {{ activeRecord.member_count }} 人</el-tag>
              <el-tag type="success">宠物 {{ activeRecord.pet_count }} 只</el-tag>
            </div>
            <dl class="family-detail-list">
              <div>
                <dt>家庭 ID</dt>
                <dd>{{ activeRecord.family_id }}</dd>
              </div>
              <div>
                <dt>拥有者</dt>
                <dd>{{ activeRecord.owner.nickname || '-' }} · {{ activeRecord.owner.mobile || '-' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(activeRecord.created_at) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDateTime(activeRecord.updated_at) }}</dd>
              </div>
            </dl>
          </section>

          <section class="family-detail__section">
            <h3>成员关系</h3>
            <div v-if="activeRecord.members.length === 0" class="family-empty-inline">暂无成员</div>
            <div v-else class="family-resource-list">
              <article v-for="member in activeRecord.members" :key="member.member_id" class="family-resource-card">
                <div class="family-resource-card__title">
                  <span>{{ member.nickname || member.mobile || member.user_id }}</span>
                  <el-tag size="small" :type="memberRoleTagType(member.role)">
                    {{ familyRoleLabel(member.role) }}
                  </el-tag>
                  <el-tag size="small" :type="inviteStatusTagType(member.invite_status)">
                    {{ inviteStatusLabel(member.invite_status) }}
                  </el-tag>
                </div>
                <div class="family-cell__meta">用户 ID：{{ member.user_id }} · 关系 ID：{{ member.member_id }}</div>
                <div class="family-cell__meta">手机号：{{ member.mobile || '-' }}</div>
                <div class="family-cell__meta">加入：{{ member.joined_at ? formatDateTime(member.joined_at) : '-' }}</div>
              </article>
            </div>
          </section>

          <section class="family-detail__section">
            <h3>家庭宠物</h3>
            <div v-if="activeRecord.pets.length === 0" class="family-empty-inline">暂无宠物</div>
            <div v-else class="family-resource-list">
              <article v-for="pet in activeRecord.pets" :key="pet.pet_id" class="family-resource-card">
                <div class="family-resource-card__title">
                  <span>{{ pet.pet_name }}</span>
                  <el-tag size="small" type="success">{{ petTypeLabel(pet.pet_type) }}</el-tag>
                  <el-tag size="small" :type="petStatusTagType(pet.status)">
                    {{ petStatusLabel(pet.status) }}
                  </el-tag>
                </div>
                <div class="family-cell__meta">宠物 ID：{{ pet.pet_id }} · 品种：{{ pet.breed || '-' }}</div>
                <div class="family-cell__meta">主人：{{ pet.owner_nickname || '-' }} · {{ pet.owner_mobile || '-' }}</div>
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
  getAdminFamily,
  listAdminFamilies,
  repairAdminFamilyOwnerMember,
  updateAdminFamilyStatus,
  type AdminFamilyMemberSnapshot,
  type AdminFamilySnapshot,
  type FamilyRole,
  type FamilyRoleFilter,
  type FamilyStatus,
  type FamilyStatusFilter,
  type PetStatus
} from '@/shared/api/adminGovernanceApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const records = ref<AdminFamilySnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminFamilySnapshot | null>(null);

const filters = reactive<{
  keyword: string;
  familyName: string;
  memberMobile: string;
  memberRole: FamilyRoleFilter;
  status: FamilyStatusFilter;
}>({
  keyword: '',
  familyName: '',
  memberMobile: '',
  memberRole: 'all',
  status: 'all'
});

const activeFamilyCount = computed(
  () => records.value.filter((record) => record.status === 1).length
);
const disabledFamilyCount = computed(
  () => records.value.filter((record) => record.status === 2).length
);
const totalMemberCount = computed(
  () => records.value.reduce((total, record) => total + record.member_count, 0)
);
const totalPetCount = computed(
  () => records.value.reduce((total, record) => total + record.pet_count, 0)
);

const summaryCards = computed(() => [
  {
    title: '家庭总数',
    description: '当前筛选条件下返回的家庭。',
    value: `${records.value.length} 个`
  },
  {
    title: '正常家庭',
    description: '状态为正常的家庭。',
    value: `${activeFamilyCount.value} 个`
  },
  {
    title: '停用家庭',
    description: '状态为停用的家庭，可在核查后恢复。',
    value: `${disabledFamilyCount.value} 个`
  },
  {
    title: '家庭宠物',
    description: '当前结果内的家庭宠物总量。',
    value: `${totalPetCount.value} 只`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminFamilies({
      keyword: filters.keyword,
      familyName: filters.familyName,
      memberMobile: filters.memberMobile,
      memberRole: filters.memberRole,
      status: filters.status
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '家庭列表加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminFamilySnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminFamily(record.family_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '家庭详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handleFamilyStatusChange(record: AdminFamilySnapshot) {
  const targetStatus = record.status === 1 ? 2 : 1;
  const actionLabel = targetStatus === 2 ? '停用' : '恢复';
  try {
    const result = await ElMessageBox.prompt(
      `请输入${actionLabel}原因。`,
      `${actionLabel}家庭`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        inputPlaceholder: '例如：异常关系核查 / 核查完成恢复'
      }
    );
    await updateAdminFamilyStatus(record.family_id, {
      status: targetStatus,
      reason: result.value
    });
    ElMessage.success(`已${actionLabel}家庭`);
    await loadRecords();
    if (activeRecord.value?.family_id === record.family_id) {
      activeRecord.value = await getAdminFamily(record.family_id);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : `${actionLabel}失败`);
    }
  }
}

async function handleOwnerRepair(record: AdminFamilySnapshot) {
  try {
    const result = await ElMessageBox.prompt(
      '将以家庭 owner_user_id 为事实来源，补齐 owner 成员并修正重复 owner 角色。',
      '修复家庭 owner 关系',
      {
        confirmButtonText: '执行修复',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入修复原因'
      }
    );
    await repairAdminFamilyOwnerMember(record.family_id, {
      reason: result.value
    });
    ElMessage.success('家庭 owner 关系已修复');
    await loadRecords();
    if (activeRecord.value?.family_id === record.family_id) {
      activeRecord.value = await getAdminFamily(record.family_id);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '家庭关系修复失败');
    }
  }
}

function familyStatusLabel(status: FamilyStatus) {
  const labelMap: Record<FamilyStatus, string> = {
    1: '正常',
    2: '停用'
  };
  return labelMap[status];
}

function familyStatusTagType(status: FamilyStatus) {
  return status === 1 ? 'success' : 'warning';
}

function familyRoleLabel(role: FamilyRole) {
  const labelMap: Record<FamilyRole, string> = {
    owner: '拥有者',
    admin: '管理员',
    member: '成员'
  };
  return labelMap[role];
}

function memberRoleTagType(role: FamilyRole) {
  if (role === 'owner') {
    return 'success';
  }
  if (role === 'admin') {
    return 'warning';
  }
  return 'info';
}

function inviteStatusLabel(inviteStatus: AdminFamilyMemberSnapshot['invite_status']) {
  const labelMap: Record<AdminFamilyMemberSnapshot['invite_status'], string> = {
    pending: '待加入',
    joined: '已加入',
    rejected: '已拒绝'
  };
  return labelMap[inviteStatus];
}

function inviteStatusTagType(inviteStatus: AdminFamilyMemberSnapshot['invite_status']) {
  if (inviteStatus === 'joined') {
    return 'success';
  }
  if (inviteStatus === 'pending') {
    return 'warning';
  }
  return 'info';
}

function petStatusLabel(status: PetStatus) {
  const labelMap: Record<PetStatus, string> = {
    active: '活跃',
    memorial: '纪念',
    rehomed: '已转交'
  };
  return labelMap[status];
}

function petStatusTagType(status: PetStatus) {
  if (status === 'active') {
    return 'success';
  }
  if (status === 'memorial') {
    return 'info';
  }
  return 'warning';
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
.family-summary,
.family-section {
  margin-top: 24px;
}

.family-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.family-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.family-filter {
  width: 138px;
}

.family-keyword {
  width: 220px;
}

.family-error {
  margin-bottom: 16px;
}

.family-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.family-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.family-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.family-cell__meta {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.family-count-line {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--pet-admin-title);
}

.family-count-line strong {
  font-size: 20px;
}

.family-chip-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.family-action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.family-detail {
  min-height: 320px;
}

.family-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.family-detail__section:first-child {
  padding-top: 0;
}

.family-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.family-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.family-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.family-detail-list div {
  display: grid;
  gap: 5px;
}

.family-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.family-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.family-empty-inline {
  padding: 14px;
  border-radius: 14px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
}

.family-resource-list {
  display: grid;
  gap: 12px;
}

.family-resource-card {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 14px;
  background: var(--pet-admin-surface);
}

.family-resource-card__title {
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
  .family-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .family-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .family-filter,
  .family-keyword {
    width: 100%;
  }

  .family-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
