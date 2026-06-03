<template>
  <section class="page-section template-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">提醒模板</p>
      <h1 class="page-section__title">提醒模板管理</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">模板 {{ templates.length }} 个</span>
        <span class="pet-admin-chip">启用 {{ enabledTemplateCount }} 个</span>
        <span class="pet-admin-chip">周期模板 {{ cycleTemplateCount }} 个</span>
        <span class="pet-admin-chip">全宠适用 {{ allPetTemplateCount }} 个</span>
      </div>
    </div>

    <div class="summary-grid template-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel template-section">
      <div class="template-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">模板配置</h2>
        </div>
        <div class="template-toolbar__actions">
          <el-select v-model="filters.reminderType" size="small" class="template-filter" placeholder="提醒类型">
            <el-option label="全部类型" value="all" />
            <el-option
              v-for="option in reminderTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.defaultReminderMode" size="small" class="template-filter" placeholder="提醒模式">
            <el-option label="全部模式" value="all" />
            <el-option label="单次" value="single" />
            <el-option label="周期" value="cycle" />
          </el-select>
          <el-select v-model="filters.applicablePetType" size="small" class="template-filter" placeholder="适用宠物">
            <el-option label="全部适用范围" value="all_pet" />
            <el-option label="所有宠物" value="all" />
            <el-option label="猫" value="cat" />
            <el-option label="狗" value="dog" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-select v-model="filters.enabled" size="small" class="template-filter" placeholder="启用状态">
            <el-option label="全部状态" value="all" />
            <el-option label="已启用" value="true" />
            <el-option label="已停用" value="false" />
          </el-select>
          <el-input v-model="filters.keyword" size="small" class="template-keyword" placeholder="模板名称 / 类型" clearable />
          <el-button :loading="isLoading" @click="loadTemplates">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="template-error"
        :closable="false"
      />

      <el-table
        :data="templates"
        v-loading="isLoading"
        row-key="template_id"
        empty-text="暂无提醒模板"
        class="template-table"
      >
        <el-table-column label="模板" min-width="260">
          <template #default="{ row }">
            <div class="template-cell">
              <div class="template-cell__title">
                <span>{{ row.template_name }}</span>
                <el-tag size="small" :type="reminderTypeTagType(row.reminder_type)">
                  {{ reminderTypeLabel(row.reminder_type) }}
                </el-tag>
              </div>
              <div class="template-cell__meta">模板 ID：{{ row.template_id }}</div>
              <div class="template-cell__meta">排序：{{ row.sort_order }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="默认规则" min-width="260">
          <template #default="{ row }">
            <div class="template-cell">
              <div class="template-cell__title">{{ reminderModeLabel(row.default_reminder_mode) }}</div>
              <div class="template-cell__meta">
                提前 {{ row.default_advance_value }} {{ unitLabel(row.default_advance_unit) }}
              </div>
              <div class="template-cell__meta">
                {{ templateCycleLabel(row) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="适用与状态" min-width="180">
          <template #default="{ row }">
            <div class="template-cell">
              <div class="template-cell__title">{{ petTypeLabel(row.applicable_pet_type) }}</div>
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
                {{ row.enabled ? '已启用' : '已停用' }}
              </el-tag>
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
            <div class="template-actions">
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

    <el-drawer v-model="detailDrawerVisible" title="提醒模板详情" size="520px">
      <div v-loading="detailLoading" class="template-detail">
        <template v-if="activeTemplate">
          <section class="template-detail__section">
            <h3>{{ activeTemplate.template_name }}</h3>
            <div class="template-detail__tags">
              <el-tag :type="reminderTypeTagType(activeTemplate.reminder_type)">
                {{ reminderTypeLabel(activeTemplate.reminder_type) }}
              </el-tag>
              <el-tag :type="activeTemplate.enabled ? 'success' : 'info'">
                {{ activeTemplate.enabled ? '已启用' : '已停用' }}
              </el-tag>
            </div>
            <dl class="template-detail-list">
              <div>
                <dt>模板 ID</dt>
                <dd>{{ activeTemplate.template_id }}</dd>
              </div>
              <div>
                <dt>适用宠物</dt>
                <dd>{{ petTypeLabel(activeTemplate.applicable_pet_type) }}</dd>
              </div>
              <div>
                <dt>提醒模式</dt>
                <dd>{{ reminderModeLabel(activeTemplate.default_reminder_mode) }}</dd>
              </div>
              <div>
                <dt>默认提前</dt>
                <dd>{{ activeTemplate.default_advance_value }} {{ unitLabel(activeTemplate.default_advance_unit) }}</dd>
              </div>
              <div>
                <dt>默认周期</dt>
                <dd>{{ templateCycleLabel(activeTemplate) }}</dd>
              </div>
              <div>
                <dt>排序</dt>
                <dd>{{ activeTemplate.sort_order }}</dd>
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

    <el-dialog v-model="templateDialogVisible" :title="editingTemplateId ? '编辑提醒模板' : '新增提醒模板'" width="560px">
      <div class="template-form">
        <label class="template-form__item">
          <span>模板名称</span>
          <el-input v-model="templateForm.templateName" maxlength="100" show-word-limit placeholder="例如：年度疫苗提醒" />
        </label>
        <div class="template-form__grid">
          <label class="template-form__item">
            <span>提醒类型</span>
            <el-select v-model="templateForm.reminderType" placeholder="选择类型">
              <el-option
                v-for="option in reminderTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>
          <label class="template-form__item">
            <span>适用宠物</span>
            <el-select v-model="templateForm.applicablePetType" placeholder="选择宠物类型">
              <el-option label="所有宠物" value="all" />
              <el-option label="猫" value="cat" />
              <el-option label="狗" value="dog" />
              <el-option label="其他" value="other" />
            </el-select>
          </label>
        </div>
        <div class="template-form__grid">
          <label class="template-form__item">
            <span>提醒模式</span>
            <el-select v-model="templateForm.defaultReminderMode" placeholder="选择模式">
              <el-option label="单次" value="single" />
              <el-option label="周期" value="cycle" />
            </el-select>
          </label>
          <label class="template-form__item">
            <span>启用状态</span>
            <el-switch v-model="templateForm.enabled" active-text="启用" inactive-text="停用" />
          </label>
        </div>
        <div class="template-form__grid">
          <label class="template-form__item">
            <span>默认提前量</span>
            <el-input-number v-model="templateForm.defaultAdvanceValue" :min="0" :step="1" controls-position="right" />
          </label>
          <label class="template-form__item">
            <span>默认提前单位</span>
            <el-select v-model="templateForm.defaultAdvanceUnit" placeholder="选择单位">
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
            </el-select>
          </label>
        </div>
        <div v-if="templateForm.defaultReminderMode === 'cycle'" class="template-form__grid">
          <label class="template-form__item">
            <span>默认周期值</span>
            <el-input-number v-model="templateForm.defaultCycleValue" :min="1" :step="1" controls-position="right" />
          </label>
          <label class="template-form__item">
            <span>默认周期单位</span>
            <el-select v-model="templateForm.defaultCycleUnit" placeholder="选择单位">
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
            </el-select>
          </label>
        </div>
        <label class="template-form__item">
          <span>排序值</span>
          <el-input-number v-model="templateForm.sortOrder" :min="0" :step="1" controls-position="right" />
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
  createAdminReminderTemplate,
  getAdminReminderTemplate,
  listAdminReminderTemplates,
  updateAdminReminderTemplate,
  updateAdminReminderTemplateStatus,
  type ReminderCycleUnit,
  type ReminderEnabledFilter,
  type ReminderMode,
  type ReminderModeFilter,
  type ReminderPetType,
  type ReminderPetTypeFilter,
  type ReminderTemplateSnapshot,
  type ReminderType,
  type ReminderTypeFilter,
  type UpsertReminderTemplatePayload
} from '@/shared/api/adminReminderApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

interface ReminderTemplateForm {
  templateName: string;
  reminderType: ReminderType;
  defaultReminderMode: ReminderMode;
  defaultAdvanceValue: number;
  defaultAdvanceUnit: ReminderCycleUnit;
  defaultCycleValue: number;
  defaultCycleUnit: ReminderCycleUnit;
  applicablePetType: ReminderPetType;
  enabled: boolean;
  sortOrder: number;
}

const reminderTypeOptions: Array<{ label: string; value: ReminderType }> = [
  { label: '疫苗', value: 'vaccine' },
  { label: '驱虫', value: 'deworming' },
  { label: '体检', value: 'examination' },
  { label: '用药', value: 'medication' },
  { label: '自定义', value: 'custom' }
];

const templates = ref<ReminderTemplateSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const templateSubmitting = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const templateDialogVisible = ref(false);
const activeTemplate = ref<ReminderTemplateSnapshot | null>(null);
const editingTemplateId = ref<string | null>(null);
const processingTemplateId = ref<string | null>(null);

const filters = reactive<{
  keyword: string;
  reminderType: ReminderTypeFilter;
  defaultReminderMode: ReminderModeFilter;
  applicablePetType: ReminderPetTypeFilter;
  enabled: ReminderEnabledFilter;
}>({
  keyword: '',
  reminderType: 'all',
  defaultReminderMode: 'all',
  applicablePetType: 'all_pet',
  enabled: 'all'
});

const templateForm = reactive<ReminderTemplateForm>(createDefaultTemplateForm());

const enabledTemplateCount = computed(
  () => templates.value.filter((template) => template.enabled).length
);
const cycleTemplateCount = computed(
  () => templates.value.filter((template) => template.default_reminder_mode === 'cycle').length
);
const allPetTemplateCount = computed(
  () => templates.value.filter((template) => template.applicable_pet_type === 'all').length
);
const disabledTemplateCount = computed(
  () => templates.value.filter((template) => !template.enabled).length
);

const summaryCards = computed(() => [
  {
    title: '模板总数',
    description: '当前筛选条件下返回的模板。',
    value: `${templates.value.length} 个`
  },
  {
    title: '启用模板',
    description: '会作为有效模板展示给后续能力使用。',
    value: `${enabledTemplateCount.value} 个`
  },
  {
    title: '周期规则',
    description: '已配置默认周期的模板。',
    value: `${cycleTemplateCount.value} 个`
  },
  {
    title: '停用模板',
    description: '保留配置但暂不参与业务选择。',
    value: `${disabledTemplateCount.value} 个`
  }
]);

onMounted(() => {
  void loadTemplates();
});

async function loadTemplates() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    templates.value = await listAdminReminderTemplates({
      keyword: filters.keyword,
      reminderType: filters.reminderType,
      defaultReminderMode: filters.defaultReminderMode,
      applicablePetType: filters.applicablePetType,
      enabled: filters.enabled
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提醒模板加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(template: ReminderTemplateSnapshot) {
  detailDrawerVisible.value = true;
  activeTemplate.value = template;
  detailLoading.value = true;
  try {
    activeTemplate.value = await getAdminReminderTemplate(template.template_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提醒模板详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openCreateDialog() {
  editingTemplateId.value = null;
  Object.assign(templateForm, createDefaultTemplateForm());
  templateDialogVisible.value = true;
}

async function openEditDialog(template: ReminderTemplateSnapshot) {
  editingTemplateId.value = template.template_id;
  templateSubmitting.value = true;
  try {
    const latestTemplate = await getAdminReminderTemplate(template.template_id);
    Object.assign(templateForm, toTemplateForm(latestTemplate));
    templateDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提醒模板详情加载失败');
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
      ? await updateAdminReminderTemplate(editingTemplateId.value, payload)
      : await createAdminReminderTemplate(payload);
    upsertTemplate(savedTemplate);
    templateDialogVisible.value = false;
    ElMessage.success(editingTemplateId.value ? '提醒模板已更新' : '提醒模板已创建');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提醒模板保存失败');
  } finally {
    templateSubmitting.value = false;
  }
}

async function toggleTemplateStatus(template: ReminderTemplateSnapshot) {
  processingTemplateId.value = template.template_id;
  try {
    const updatedTemplate = await updateAdminReminderTemplateStatus(template.template_id, !template.enabled);
    upsertTemplate(updatedTemplate);
    ElMessage.success(updatedTemplate.enabled ? '提醒模板已启用' : '提醒模板已停用');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提醒模板状态更新失败');
  } finally {
    processingTemplateId.value = null;
  }
}

function createDefaultTemplateForm(): ReminderTemplateForm {
  return {
    templateName: '',
    reminderType: 'vaccine',
    defaultReminderMode: 'cycle',
    defaultAdvanceValue: 7,
    defaultAdvanceUnit: 'day',
    defaultCycleValue: 12,
    defaultCycleUnit: 'month',
    applicablePetType: 'all',
    enabled: true,
    sortOrder: 0
  };
}

function toTemplateForm(template: ReminderTemplateSnapshot): ReminderTemplateForm {
  return {
    templateName: template.template_name,
    reminderType: template.reminder_type,
    defaultReminderMode: template.default_reminder_mode,
    defaultAdvanceValue: template.default_advance_value,
    defaultAdvanceUnit: template.default_advance_unit,
    defaultCycleValue: template.default_cycle_value ?? 1,
    defaultCycleUnit: template.default_cycle_unit ?? 'month',
    applicablePetType: template.applicable_pet_type,
    enabled: template.enabled,
    sortOrder: template.sort_order
  };
}

function validateTemplateForm() {
  if (!templateForm.templateName.trim()) {
    ElMessage.warning('请填写模板名称');
    return false;
  }
  if (templateForm.defaultAdvanceValue < 0) {
    ElMessage.warning('默认提前量不能小于 0');
    return false;
  }
  if (templateForm.sortOrder < 0) {
    ElMessage.warning('排序值不能小于 0');
    return false;
  }
  if (templateForm.defaultReminderMode === 'cycle' && templateForm.defaultCycleValue <= 0) {
    ElMessage.warning('周期模板必须填写大于 0 的默认周期值');
    return false;
  }
  return true;
}

function toUpsertPayload(): UpsertReminderTemplatePayload {
  return {
    template_name: templateForm.templateName.trim(),
    reminder_type: templateForm.reminderType,
    default_reminder_mode: templateForm.defaultReminderMode,
    default_advance_value: templateForm.defaultAdvanceValue,
    default_advance_unit: templateForm.defaultAdvanceUnit,
    default_cycle_value: templateForm.defaultReminderMode === 'cycle' ? templateForm.defaultCycleValue : null,
    default_cycle_unit: templateForm.defaultReminderMode === 'cycle' ? templateForm.defaultCycleUnit : null,
    applicable_pet_type: templateForm.applicablePetType,
    enabled: templateForm.enabled,
    sort_order: templateForm.sortOrder
  };
}

function upsertTemplate(template: ReminderTemplateSnapshot) {
  const templateIndex = templates.value.findIndex((item) => item.template_id === template.template_id);
  if (templateIndex >= 0) {
    templates.value.splice(templateIndex, 1, template);
    return;
  }
  templates.value.unshift(template);
}

function reminderTypeLabel(reminderType: ReminderType) {
  const labelMap: Record<ReminderType, string> = {
    vaccine: '疫苗',
    deworming: '驱虫',
    examination: '体检',
    medication: '用药',
    custom: '自定义'
  };
  return labelMap[reminderType];
}

function reminderTypeTagType(reminderType: ReminderType) {
  if (reminderType === 'vaccine' || reminderType === 'deworming') {
    return 'success';
  }
  if (reminderType === 'medication') {
    return 'danger';
  }
  if (reminderType === 'custom') {
    return 'warning';
  }
  return 'info';
}

function reminderModeLabel(mode: ReminderMode) {
  return mode === 'cycle' ? '周期提醒' : '单次提醒';
}

function templateCycleLabel(template: ReminderTemplateSnapshot) {
  if (template.default_reminder_mode === 'single') {
    return '不重复';
  }
  if (!template.default_cycle_value || !template.default_cycle_unit) {
    return '周期未配置';
  }
  return `每 ${template.default_cycle_value} ${unitLabel(template.default_cycle_unit)}`;
}

function unitLabel(unit: ReminderCycleUnit) {
  const labelMap: Record<ReminderCycleUnit, string> = {
    day: '天',
    week: '周',
    month: '月'
  };
  return labelMap[unit];
}

function petTypeLabel(petType: ReminderPetType) {
  const labelMap: Record<ReminderPetType, string> = {
    all: '所有宠物',
    cat: '猫',
    dog: '狗',
    other: '其他'
  };
  return labelMap[petType];
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.template-summary,
.template-section {
  margin-top: 24px;
}

.template-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.template-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.template-filter {
  width: 132px;
}

.template-keyword {
  width: 220px;
}

.template-error {
  margin-bottom: 16px;
}

.template-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.template-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.template-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.template-cell__meta {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.template-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-detail {
  min-height: 320px;
}

.template-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.template-detail__section:first-child {
  padding-top: 0;
}

.template-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.template-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.template-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.template-detail-list div {
  display: grid;
  gap: 5px;
}

.template-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.template-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.template-form {
  display: grid;
  gap: 16px;
}

.template-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.template-form__item {
  display: grid;
  gap: 8px;
  color: var(--pet-admin-title);
  font-size: 13px;
  font-weight: 700;
}

.template-form__item :deep(.el-input-number),
.template-form__item :deep(.el-select) {
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
:deep(.el-input-number),
:deep(.el-dialog),
:deep(.el-drawer) {
  border-radius: 14px;
}

:deep(.el-dialog__title),
:deep(.el-drawer__title) {
  color: var(--pet-admin-title);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .template-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .template-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .template-filter,
  .template-keyword {
    width: 100%;
  }

  .template-form__grid {
    grid-template-columns: 1fr;
  }

  .template-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
