<template>
  <section class="page-section daily-admin-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">萌宠日常</p>
      <h1 class="page-section__title">萌宠日常管理</h1>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">日常 {{ records.length }} 条</span>
        <span class="pet-admin-chip">公开 {{ publicCount }} 条</span>
        <span class="pet-admin-chip">同步社区 {{ communitySyncCount }} 条</span>
        <span class="pet-admin-chip">带媒体 {{ mediaRecordCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid daily-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel daily-section">
      <div class="daily-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">日常内容队列</h2>
        </div>
        <div class="daily-toolbar__actions">
          <el-select v-model="filters.visibility" size="small" class="daily-filter" placeholder="可见范围">
            <el-option label="全部范围" value="all" />
            <el-option label="仅自己" value="private" />
            <el-option label="家庭可见" value="family" />
            <el-option label="公开" value="public" />
          </el-select>
          <el-select v-model="filters.syncToCommunity" size="small" class="daily-filter" placeholder="社区同步">
            <el-option label="全部同步" value="all" />
            <el-option label="已同步" value="true" />
            <el-option label="未同步" value="false" />
          </el-select>
          <el-input v-model="filters.petId" size="small" class="daily-filter" placeholder="宠物 ID" clearable />
          <el-input v-model="filters.authorUserId" size="small" class="daily-filter" placeholder="作者 ID" clearable />
          <el-input v-model="filters.keyword" size="small" class="daily-keyword" placeholder="内容 / 宠物 / 作者" clearable />
          <el-button :loading="isLoading" @click="loadRecords">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="daily-error"
        :closable="false"
      />

      <el-table
        :data="records"
        v-loading="isLoading"
        row-key="daily_log.daily_log_id"
        empty-text="暂无萌宠日常"
        class="daily-table"
      >
        <el-table-column label="日常内容" min-width="340">
          <template #default="{ row }">
            <div class="daily-cell">
              <div class="daily-cell__title">
                <span>#{{ row.daily_log.daily_log_id }}</span>
                <el-tag size="small" :type="visibilityTagType(row.daily_log.visibility)">
                  {{ visibilityLabel(row.daily_log.visibility) }}
                </el-tag>
                <el-tag v-if="row.daily_log.sync_to_community" size="small" type="success">
                  已同步社区
                </el-tag>
              </div>
              <div class="daily-cell__detail">{{ summarizeContent(row.daily_log.content) }}</div>
              <div class="daily-tag-list">
                <el-tag v-for="tag in row.daily_log.tags" :key="tag" size="small" type="info">
                  {{ tag }}
                </el-tag>
                <span v-if="row.daily_log.tags.length === 0">暂无标签</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="宠物归属" min-width="220">
          <template #default="{ row }">
            <div class="daily-cell">
              <div class="daily-cell__title">{{ row.pet.pet_name }}</div>
              <div class="daily-cell__meta">
                {{ petTypeLabel(row.pet.pet_type) }} · {{ row.pet.family_name || '暂无家庭' }}
              </div>
              <div class="daily-cell__meta">
                主人：{{ row.pet.owner_nickname || '未知主人' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="作者" min-width="180">
          <template #default="{ row }">
            <div class="daily-cell">
              <div class="daily-cell__title">{{ row.author.nickname || '未知用户' }}</div>
              <div class="daily-cell__meta">ID：{{ row.author.user_id }}</div>
              <div v-if="row.author.mobile" class="daily-cell__meta">{{ row.author.mobile }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="媒体" width="120">
          <template #default="{ row }">
            <div class="daily-count-stack">
              <strong>{{ row.daily_log.media_assets.length }}</strong>
              <span>个媒体</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="daily-cell">
              <div class="daily-cell__meta">发生：{{ formatDateTime(row.daily_log.happened_at) }}</div>
              <div class="daily-cell__meta">创建：{{ formatDateTime(row.daily_log.created_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="萌宠日常详情" size="520px">
      <div v-loading="detailLoading" class="daily-detail">
        <template v-if="activeRecord">
          <section class="daily-detail__section">
            <h3>内容</h3>
            <p class="daily-detail__content">{{ activeRecord.daily_log.content }}</p>
            <div class="daily-detail__tags">
              <el-tag :type="visibilityTagType(activeRecord.daily_log.visibility)">
                {{ visibilityLabel(activeRecord.daily_log.visibility) }}
              </el-tag>
              <el-tag v-if="activeRecord.daily_log.sync_to_community" type="success">
                社区帖子 {{ activeRecord.daily_log.community_post_id || '-' }}
              </el-tag>
            </div>
          </section>

          <section class="daily-detail__section">
            <h3>宠物与作者</h3>
            <dl class="daily-detail-list">
              <div>
                <dt>宠物</dt>
                <dd>{{ activeRecord.pet.pet_name }} · {{ petTypeLabel(activeRecord.pet.pet_type) }}</dd>
              </div>
              <div>
                <dt>家庭</dt>
                <dd>{{ activeRecord.pet.family_name || '-' }}</dd>
              </div>
              <div>
                <dt>主人</dt>
                <dd>{{ activeRecord.pet.owner_nickname || '-' }} · {{ activeRecord.pet.owner_mobile || '-' }}</dd>
              </div>
              <div>
                <dt>作者</dt>
                <dd>{{ activeRecord.author.nickname || '-' }} · {{ activeRecord.author.mobile || '-' }}</dd>
              </div>
              <div>
                <dt>发生时间</dt>
                <dd>{{ formatDateTime(activeRecord.daily_log.happened_at) }}</dd>
              </div>
            </dl>
          </section>

          <section class="daily-detail__section">
            <h3>媒体</h3>
            <div v-if="activeRecord.daily_log.media_assets.length === 0" class="daily-empty-inline">
              暂无媒体
            </div>
            <div v-else class="daily-asset-list">
              <article
                v-for="asset in activeRecord.daily_log.media_assets"
                :key="asset.asset_id"
                class="daily-asset"
              >
                <div class="daily-asset__title">
                  <span>{{ asset.file_name }}</span>
                  <el-tag size="small" type="info">{{ mediaTypeLabel(asset.media_type) }}</el-tag>
                </div>
                <div class="daily-cell__meta">
                  {{ asset.content_type }} · {{ formatFileSize(asset.file_size) }}
                </div>
                <div class="daily-cell__meta">
                  审核：{{ asset.review_status }} · 状态：{{ asset.upload_status }}
                </div>
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
  getAdminDailyLog,
  listAdminDailyLogs,
  type AdminDailyLogSnapshot,
  type BooleanFilter,
  type DailyLogVisibility,
  type DailyLogVisibilityFilter,
  type MediaType
} from '@/shared/api/adminContentApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const records = ref<AdminDailyLogSnapshot[]>([]);
const isLoading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const detailDrawerVisible = ref(false);
const activeRecord = ref<AdminDailyLogSnapshot | null>(null);

const filters = reactive<{
  visibility: DailyLogVisibilityFilter;
  syncToCommunity: BooleanFilter;
  petId: string;
  authorUserId: string;
  keyword: string;
}>({
  visibility: 'all',
  syncToCommunity: 'all',
  petId: '',
  authorUserId: '',
  keyword: ''
});

const publicCount = computed(
  () => records.value.filter((record) => record.daily_log.visibility === 'public').length
);
const communitySyncCount = computed(
  () => records.value.filter((record) => record.daily_log.sync_to_community).length
);
const mediaRecordCount = computed(
  () => records.value.filter((record) => record.daily_log.media_assets.length > 0).length
);
const familyVisibleCount = computed(
  () => records.value.filter((record) => record.daily_log.visibility === 'family').length
);

const summaryCards = computed(() => [
  {
    title: '内容总数',
    description: '当前筛选条件下返回的日常内容。',
    value: `${records.value.length} 条`
  },
  {
    title: '公开内容',
    description: '可被公开浏览的萌宠日常。',
    value: `${publicCount.value} 条`
  },
  {
    title: '家庭可见',
    description: '仅家庭成员可见的日常。',
    value: `${familyVisibleCount.value} 条`
  },
  {
    title: '媒体内容',
    description: '含图片或视频的日常。',
    value: `${mediaRecordCount.value} 条`
  }
]);

onMounted(() => {
  void loadRecords();
});

async function loadRecords() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    records.value = await listAdminDailyLogs({
      visibility: filters.visibility,
      syncToCommunity: filters.syncToCommunity,
      petId: filters.petId,
      authorUserId: filters.authorUserId,
      keyword: filters.keyword
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '萌宠日常加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function openDetail(record: AdminDailyLogSnapshot) {
  detailDrawerVisible.value = true;
  activeRecord.value = record;
  detailLoading.value = true;
  try {
    activeRecord.value = await getAdminDailyLog(record.daily_log.daily_log_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '萌宠日常详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function visibilityLabel(visibility: DailyLogVisibility) {
  const labelMap: Record<DailyLogVisibility, string> = {
    private: '仅自己',
    family: '家庭可见',
    public: '公开'
  };
  return labelMap[visibility];
}

function visibilityTagType(visibility: DailyLogVisibility) {
  if (visibility === 'public') {
    return 'success';
  }
  if (visibility === 'family') {
    return 'warning';
  }
  return 'info';
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗'
  };
  return labelMap[petType] ?? petType;
}

function mediaTypeLabel(mediaType: MediaType) {
  const labelMap: Record<MediaType, string> = {
    image: '图片',
    video: '视频',
    file: '文件'
  };
  return labelMap[mediaType];
}

function summarizeContent(content: string) {
  return content.length > 96 ? `${content.slice(0, 96)}...` : content;
}

function formatFileSize(fileSize: number) {
  if (fileSize < 1024) {
    return `${fileSize} B`;
  }
  if (fileSize < 1024 * 1024) {
    return `${(fileSize / 1024).toFixed(1)} KB`;
  }
  return `${(fileSize / 1024 / 1024).toFixed(1)} MB`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}
</script>

<style scoped>
.daily-summary,
.daily-section {
  margin-top: 24px;
}

.daily-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.daily-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.daily-filter {
  width: 132px;
}

.daily-keyword {
  width: 210px;
}

.daily-error {
  margin-bottom: 16px;
}

.daily-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.daily-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.daily-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.daily-cell__meta,
.daily-cell__detail,
.daily-tag-list {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.daily-cell__detail {
  color: var(--pet-admin-body);
}

.daily-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.daily-count-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.daily-count-stack strong {
  color: var(--pet-admin-title);
  font-size: 18px;
}

.daily-count-stack span {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.daily-detail {
  min-height: 320px;
}

.daily-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.daily-detail__section:first-child {
  padding-top: 0;
}

.daily-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.daily-detail__content {
  margin: 0 0 14px;
  color: var(--pet-admin-body);
  line-height: 1.8;
}

.daily-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.daily-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.daily-detail-list div {
  display: grid;
  gap: 5px;
}

.daily-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.daily-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.daily-empty-inline {
  padding: 16px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.daily-asset-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.daily-asset {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface);
}

.daily-asset__title {
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
  .daily-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .daily-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .daily-filter,
  .daily-keyword {
    width: 100%;
  }

  .daily-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
