<template>
  <section class="page-section pet-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">宠物主档</p>
      <h1 class="page-section__title">查询宠物档案、主人和家庭归属</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">宠物 {{ records.length }} 只</span>
        <span class="pet-admin-chip">活跃 {{ activePetCount }} 只</span>
        <span class="pet-admin-chip">猫 {{ catCount }} 只</span>
        <span class="pet-admin-chip">狗 {{ dogCount }} 只</span>
      </div>
    </div>

    <div class="summary-grid pet-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel pet-section">
      <div class="pet-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">宠物档案查询</h2>
        </div>
        <div class="pet-toolbar__actions">
          <el-input v-model="filters.keyword" size="small" class="pet-keyword" placeholder="宠物 / 品种 / 家庭 / 主人" clearable />
          <el-input v-model="filters.petName" size="small" class="pet-filter" placeholder="宠物名" clearable />
          <el-select v-model="filters.petType" size="small" class="pet-filter" placeholder="宠物类型">
            <el-option label="全部类型" value="all" />
            <el-option label="猫" value="cat" />
            <el-option label="狗" value="dog" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-select v-model="filters.status" size="small" class="pet-filter" placeholder="宠物状态">
            <el-option label="全部状态" value="all" />
            <el-option label="活跃" value="active" />
            <el-option label="纪念" value="memorial" />
            <el-option label="已转交" value="rehomed" />
          </el-select>
          <el-input v-model="filters.ownerMobile" size="small" class="pet-filter" placeholder="主人手机号" clearable />
          <el-input v-model="filters.familyId" size="small" class="pet-filter" placeholder="家庭 ID" clearable />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="pet-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="pet.pet_id"
        empty-text="暂无宠物"
        class="pet-table"
      >
        <el-table-column label="宠物" min-width="280">
          <template #default="{ row }">
            <div class="pet-cell">
              <div class="pet-cell__title">
                <span>{{ row.pet.pet_name }}</span>
                <el-tag size="small" type="success">{{ petTypeLabel(row.pet.pet_type) }}</el-tag>
                <el-tag size="small" :type="petStatusTagType(row.pet.status)">
                  {{ petStatusLabel(row.pet.status) }}
                </el-tag>
              </div>
              <div class="pet-cell__meta">ID：{{ row.pet.pet_id }} · 品种：{{ row.pet.breed || '-' }}</div>
              <div class="pet-cell__meta">
                体重：{{ row.pet.weight_kg || '-' }} kg · 绝育：{{ neuterStatusLabel(row.pet.neuter_status) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="主人" min-width="210">
          <template #default="{ row }">
            <div class="pet-cell">
              <div class="pet-cell__title">{{ row.owner.nickname || '未知用户' }}</div>
              <div class="pet-cell__meta">ID：{{ row.owner.user_id }}</div>
              <div v-if="row.owner.mobile" class="pet-cell__meta">{{ row.owner.mobile }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="家庭归属" min-width="240">
          <template #default="{ row }">
            <div class="pet-cell">
              <template v-if="row.family">
                <div class="pet-cell__title">
                  <span>{{ row.family.family_name || '未命名家庭' }}</span>
                  <el-tag size="small" :type="familyStatusTagType(row.family.status)">
                    {{ familyStatusLabel(row.family.status) }}
                  </el-tag>
                </div>
                <div class="pet-cell__meta">家庭 ID：{{ row.family.family_id }}</div>
                <div class="pet-cell__meta">成员 {{ row.family.member_count }} 人</div>
              </template>
              <span v-else class="pet-cell__meta">暂无家庭</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="日期" width="210">
          <template #default="{ row }">
            <div class="pet-cell">
              <div class="pet-cell__meta">生日：{{ formatDate(row.pet.birthday) }}</div>
              <div class="pet-cell__meta">到家：{{ formatDate(row.pet.adopt_date) }}</div>
              <div class="pet-cell__meta">更新：{{ formatDateTime(row.pet.updated_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="健康提示" min-width="220">
          <template #default="{ row }">
            <div class="pet-cell">
              <div class="pet-cell__meta">过敏：{{ summarizeText(row.pet.allergy_notes) }}</div>
              <div class="pet-cell__meta">病史：{{ summarizeText(row.pet.medical_history) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="188" fixed="right">
          <template #default="{ row }">
            <div class="pet-action-cell">
              <el-button size="small" @click="openDetail(row)">查看详情</el-button>
              <el-dropdown trigger="click" @command="handlePetRepair(row, $event)">
                <el-button size="small">修复数据</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="family_missing">家庭缺失</el-dropdown-item>
                    <el-dropdown-item command="owner_member_missing">主人成员关系</el-dropdown-item>
                    <el-dropdown-item command="current_pet_context">当前宠物指针</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="宠物档案详情" size="560px">
      <div v-loading="detailLoading" class="pet-detail">
        <template v-if="activeRecord">
          <section class="pet-detail__section">
            <h3>{{ activeRecord.pet.pet_name }}</h3>
            <div class="pet-detail__tags">
              <el-tag type="success">{{ petTypeLabel(activeRecord.pet.pet_type) }}</el-tag>
              <el-tag :type="petStatusTagType(activeRecord.pet.status)">
                {{ petStatusLabel(activeRecord.pet.status) }}
              </el-tag>
              <el-tag type="info">{{ neuterStatusLabel(activeRecord.pet.neuter_status) }}</el-tag>
            </div>
            <dl class="pet-detail-list">
              <div>
                <dt>宠物 ID</dt>
                <dd>{{ activeRecord.pet.pet_id }}</dd>
              </div>
              <div>
                <dt>品种 / 性别</dt>
                <dd>{{ activeRecord.pet.breed || '-' }} / {{ genderLabel(activeRecord.pet.gender) }}</dd>
              </div>
              <div>
                <dt>生日 / 到家日期</dt>
                <dd>{{ formatDate(activeRecord.pet.birthday) }} / {{ formatDate(activeRecord.pet.adopt_date) }}</dd>
              </div>
              <div>
                <dt>体重</dt>
                <dd>{{ activeRecord.pet.weight_kg || '-' }} kg</dd>
              </div>
              <div>
                <dt>创建 / 更新</dt>
                <dd>{{ formatDateTime(activeRecord.pet.created_at) }} / {{ formatDateTime(activeRecord.pet.updated_at) }}</dd>
              </div>
            </dl>
          </section>

          <section class="pet-detail__section">
            <h3>主人与家庭</h3>
            <dl class="pet-detail-list">
              <div>
                <dt>主人</dt>
                <dd>{{ activeRecord.owner.nickname || '-' }} · {{ activeRecord.owner.mobile || '-' }}</dd>
              </div>
              <div>
                <dt>主人 ID</dt>
                <dd>{{ activeRecord.owner.user_id }}</dd>
              </div>
              <div>
                <dt>家庭</dt>
                <dd>
                  <template v-if="activeRecord.family">
                    {{ activeRecord.family.family_name || '-' }} · {{ familyStatusLabel(activeRecord.family.status) }}
                  </template>
                  <template v-else>-</template>
                </dd>
              </div>
              <div>
                <dt>家庭成员数</dt>
                <dd>{{ activeRecord.family ? `${activeRecord.family.member_count} 人` : '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="pet-detail__section">
            <h3>健康提示</h3>
            <dl class="pet-detail-list">
              <div>
                <dt>过敏信息</dt>
                <dd>{{ activeRecord.pet.allergy_notes || '-' }}</dd>
              </div>
              <div>
                <dt>重要病史</dt>
                <dd>{{ activeRecord.pet.medical_history || '-' }}</dd>
              </div>
              <div>
                <dt>头像地址</dt>
                <dd>{{ activeRecord.pet.avatar_url || '-' }}</dd>
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
  getAdminPet,
  listAdminPets,
  repairAdminPet,
  type AdminPetSnapshot,
  type FamilyStatus,
  type PetStatus,
  type PetStatusFilter,
  type PetTypeFilter
} from '@/shared/api/adminGovernanceApi';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const records = ref<AdminPetSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminPetSnapshot | null>(null);

const filters = reactive<{
  keyword: string;
  petName: string;
  petType: PetTypeFilter;
  status: PetStatusFilter;
  ownerMobile: string;
  familyId: string;
}>({
  keyword: '',
  petName: '',
  petType: 'all',
  status: 'all',
  ownerMobile: '',
  familyId: ''
});

const activePetCount = computed(
  () => records.value.filter((record) => record.pet.status === 'active').length
);
const catCount = computed(
  () => records.value.filter((record) => record.pet.pet_type === 'cat').length
);
const dogCount = computed(
  () => records.value.filter((record) => record.pet.pet_type === 'dog').length
);
const familyLinkedCount = computed(
  () => records.value.filter((record) => record.family !== null).length
);

const summaryCards = computed(() => [
  {
    title: '宠物总数',
    description: '当前筛选条件下返回的宠物。',
    value: `${records.value.length} 只`
  },
  {
    title: '活跃宠物',
    description: '状态为 active 的宠物。',
    value: `${activePetCount.value} 只`
  },
  {
    title: '家庭归属',
    description: '已关联家庭上下文的宠物。',
    value: `${familyLinkedCount.value} 只`
  },
  {
    title: '非活跃状态',
    description: '纪念或转交状态的宠物，仍可执行明确的问题数据修复。',
    value: `${records.value.length - activePetCount.value} 只`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminPets({
      keyword: filters.keyword,
      petName: filters.petName,
      petType: filters.petType,
      status: filters.status,
      ownerMobile: filters.ownerMobile,
      familyId: filters.familyId
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '宠物列表加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminPetSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminPet(record.pet.pet_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '宠物详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handlePetRepair(record: AdminPetSnapshot, repairType: string) {
  const labelMap: Record<string, string> = {
    family_missing: '家庭缺失',
    owner_member_missing: '主人成员关系',
    current_pet_context: '当前宠物指针'
  };
  try {
    const result = await ElMessageBox.prompt(
      `将执行「${labelMap[repairType] ?? repairType}」修复，并写入后台审计日志。`,
      '修复宠物问题数据',
      {
        confirmButtonText: '执行修复',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入修复原因'
      }
    );
    await repairAdminPet(record.pet.pet_id, {
      repairType: repairType as 'family_missing' | 'owner_member_missing' | 'current_pet_context',
      reason: result.value
    });
    ElMessage.success('宠物问题数据已修复');
    await loadRecords();
    if (activeRecord.value?.pet.pet_id === record.pet.pet_id) {
      activeRecord.value = await getAdminPet(record.pet.pet_id);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '宠物问题数据修复失败');
    }
  }
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

function familyStatusLabel(status: FamilyStatus | null) {
  if (status === 1) {
    return '正常';
  }
  if (status === 2) {
    return '停用';
  }
  return '未知状态';
}

function familyStatusTagType(status: FamilyStatus | null) {
  return status === 1 ? 'success' : 'warning';
}

function neuterStatusLabel(neuterStatus: string) {
  const labelMap: Record<string, string> = {
    unknown: '绝育未知',
    completed: '已绝育',
    pending: '未绝育'
  };
  return labelMap[neuterStatus] ?? neuterStatus;
}

function genderLabel(gender: string | null) {
  if (!gender) {
    return '-';
  }
  const labelMap: Record<string, string> = {
    male: '公',
    female: '母',
    unknown: '未知'
  };
  return labelMap[gender] ?? gender;
}

function summarizeText(value: string | null) {
  if (!value) {
    return '-';
  }
  return value.length > 28 ? `${value.slice(0, 28)}...` : value;
}

function formatDate(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('zh-CN');
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.pet-summary,
.pet-section {
  margin-top: 24px;
}

.pet-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.pet-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.pet-filter {
  width: 136px;
}

.pet-keyword {
  width: 220px;
}

.pet-error {
  margin-bottom: 16px;
}

.pet-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.pet-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.pet-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.pet-cell__meta {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.pet-action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pet-detail {
  min-height: 320px;
}

.pet-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.pet-detail__section:first-child {
  padding-top: 0;
}

.pet-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.pet-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.pet-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.pet-detail-list div {
  display: grid;
  gap: 5px;
}

.pet-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.pet-detail-list dd {
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
  .pet-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .pet-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .pet-filter,
  .pet-keyword {
    width: 100%;
  }

  .pet-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
