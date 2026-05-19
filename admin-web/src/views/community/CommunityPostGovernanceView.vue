<template>
  <section class="page-section community-page">
    <div class="pet-admin-hero">
      <p class="page-section__eyebrow">社区治理</p>
      <h1 class="page-section__title">查询帖子内容，并按后台治理结论下架或恢复</h1>
      <p class="page-section__description">
        这里承接真实后台帖子治理接口，展示作者、宠物、话题和互动数据；状态写操作只提交下架或恢复，并同步查看审计记录。
      </p>
      <div class="pet-admin-chip-grid">
        <span class="pet-admin-chip">帖子 {{ posts.length }} 条</span>
        <span class="pet-admin-chip">公开中 {{ approvedCount }} 条</span>
        <span class="pet-admin-chip">待审核 {{ pendingCount }} 条</span>
        <span class="pet-admin-chip">已下架 {{ rejectedCount }} 条</span>
      </div>
    </div>

    <div class="summary-grid community-summary">
      <article v-for="item in summaryCards" :key="item.title" class="summary-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <article class="pet-admin-panel community-section">
      <div class="community-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">帖子治理队列</h2>
          <p class="pet-admin-panel__description">
            按内容类型、审核状态、可见范围、作者、话题或关键词筛选。列表与详情均来自后台社区管理接口。
          </p>
        </div>
        <div class="community-toolbar__actions">
          <el-select v-model="filters.postType" size="small" class="community-filter" placeholder="内容类型">
            <el-option label="全部类型" value="all" />
            <el-option label="图文" value="image_text" />
            <el-option label="视频" value="video" />
            <el-option label="问答" value="qa" />
            <el-option label="经验" value="experience" />
          </el-select>
          <el-select v-model="filters.reviewStatus" size="small" class="community-filter" placeholder="审核状态">
            <el-option label="全部状态" value="all" />
            <el-option label="待审核" value="pending_review" />
            <el-option label="公开中" value="approved" />
            <el-option label="已下架" value="rejected" />
          </el-select>
          <el-select v-model="filters.visibility" size="small" class="community-filter" placeholder="可见范围">
            <el-option label="全部范围" value="all" />
            <el-option label="公开" value="public" />
            <el-option label="粉丝可见" value="follower" />
          </el-select>
          <el-input v-model="filters.authorUserId" size="small" class="community-filter" placeholder="作者 ID" clearable />
          <el-input v-model="filters.topicId" size="small" class="community-filter" placeholder="话题 ID" clearable />
          <el-input v-model="filters.keyword" size="small" class="community-keyword" placeholder="标题 / 内容关键词" clearable />
          <el-button :loading="isLoading || auditLogLoading" @click="loadPage">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="community-error"
        :closable="false"
      />

      <el-table
        :data="posts"
        v-loading="isLoading"
        row-key="post_id"
        empty-text="暂无社区帖子"
        class="community-table"
      >
        <el-table-column label="帖子内容" min-width="340">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__title">
                <span>{{ row.title }}</span>
                <el-tag size="small" type="info">{{ postTypeLabel(row.post_type) }}</el-tag>
                <el-tag size="small" :type="reviewStatusTagType(row.review_status)">
                  {{ reviewStatusLabel(row.review_status) }}
                </el-tag>
              </div>
              <div class="community-cell__detail">{{ summarizeText(row.content, 104) }}</div>
              <div class="community-cell__meta">#{{ row.post_id }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="作者 / 宠物" min-width="230">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__title">{{ row.author.nickname || '未知作者' }}</div>
              <div class="community-cell__meta">作者 ID：{{ row.author.user_id }}</div>
              <div class="community-cell__meta">
                宠物：{{ row.pet ? `${row.pet.pet_name} · ${petTypeLabel(row.pet.pet_type)}` : '未关联宠物' }}
              </div>
              <div v-if="row.pet?.breed" class="community-cell__meta">品种：{{ row.pet.breed }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="话题 / 范围" min-width="210">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__title">{{ row.topic?.topic_name || '未关联话题' }}</div>
              <div class="community-cell__meta">话题 ID：{{ row.topic?.topic_id || '-' }}</div>
              <div class="community-cell__meta">
                {{ visibilityLabel(row.visibility) }} · {{ row.city_code || '未知城市' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="互动" width="150">
          <template #default="{ row }">
            <div class="community-count-stack">
              <span><strong>{{ row.like_count }}</strong> 赞</span>
              <span><strong>{{ row.comment_count }}</strong> 评论</span>
              <span><strong>{{ row.favorite_count }}</strong> 收藏</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__meta">发布：{{ formatDateTime(row.published_at) }}</div>
              <div class="community-cell__meta">创建：{{ formatDateTime(row.created_at) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="community-actions">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button
                size="small"
                :type="row.review_status === 'rejected' ? 'primary' : 'danger'"
                :loading="processingPostId === row.post_id"
                @click="openStatusDialog(row)"
              >
                {{ row.review_status === 'rejected' ? '恢复' : '下架' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article class="pet-admin-panel community-section">
      <div class="community-toolbar">
        <div>
          <h2 class="pet-admin-panel__title">帖子治理审计</h2>
          <p class="pet-admin-panel__description">
            查询服务端记录的社区帖子治理动作，包含操作者、目标、动作编码、详情和时间。
          </p>
        </div>
        <div class="community-toolbar__actions">
          <el-select v-model="auditLogFilters.action" size="small" class="community-audit-filter" placeholder="动作">
            <el-option label="全部动作" value="all" />
            <el-option label="帖子下架" value="community_post_take_down" />
            <el-option label="帖子恢复" value="community_post_restore" />
          </el-select>
          <el-input v-model="auditLogFilters.operatorId" size="small" class="community-audit-filter" placeholder="操作者" clearable />
          <el-button :loading="auditLogLoading" @click="loadAuditLogs">刷新审计</el-button>
        </div>
      </div>

      <el-alert
        v-if="auditLogErrorMessage"
        :title="auditLogErrorMessage"
        type="error"
        show-icon
        class="community-error"
        :closable="false"
      />

      <el-table
        :data="auditLogs"
        v-loading="auditLogLoading"
        row-key="audit_log_id"
        empty-text="暂无帖子治理审计记录"
        class="community-table"
      >
        <el-table-column label="操作" min-width="260">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__title">
                <span>{{ auditActionLabel(row.action) }}</span>
                <el-tag size="small" type="info">{{ row.target_type }}</el-tag>
              </div>
              <div class="community-cell__meta">目标 ID：{{ row.target_id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }">
            <div class="community-cell">
              <div class="community-cell__title">{{ row.operator_id }}</div>
              <div class="community-cell__meta">{{ row.ip_address || '未知 IP' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="320">
          <template #default="{ row }">
            <pre class="community-audit-detail">{{ formatAuditDetail(row.detail_json) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-drawer v-model="detailDrawerVisible" title="社区帖子详情" size="560px">
      <div v-loading="detailLoading" class="community-detail">
        <template v-if="activePost">
          <section class="community-detail__section">
            <div class="community-detail__heading">
              <h3>{{ activePost.title }}</h3>
              <el-tag :type="reviewStatusTagType(activePost.review_status)">
                {{ reviewStatusLabel(activePost.review_status) }}
              </el-tag>
            </div>
            <p class="community-detail__content">{{ activePost.content }}</p>
            <div class="community-detail__tags">
              <el-tag type="info">{{ postTypeLabel(activePost.post_type) }}</el-tag>
              <el-tag>{{ visibilityLabel(activePost.visibility) }}</el-tag>
              <el-tag v-if="activePost.source_daily_log_id" type="success">
                日常同步 {{ activePost.source_daily_log_id }}
              </el-tag>
            </div>
          </section>

          <section class="community-detail__section">
            <h3>作者、宠物与话题</h3>
            <dl class="community-detail-list">
              <div>
                <dt>作者</dt>
                <dd>{{ activePost.author.nickname || '-' }} · {{ activePost.author.user_id }}</dd>
              </div>
              <div>
                <dt>宠物</dt>
                <dd>
                  {{ activePost.pet ? `${activePost.pet.pet_name} · ${petTypeLabel(activePost.pet.pet_type)}` : '-' }}
                  <span v-if="activePost.pet?.breed"> · {{ activePost.pet.breed }}</span>
                </dd>
              </div>
              <div>
                <dt>话题</dt>
                <dd>{{ activePost.topic?.topic_name || '-' }} · {{ activePost.topic?.topic_id || '-' }}</dd>
              </div>
              <div>
                <dt>城市</dt>
                <dd>{{ activePost.city_code || '-' }}</dd>
              </div>
              <div>
                <dt>发布时间</dt>
                <dd>{{ formatDateTime(activePost.published_at) }}</dd>
              </div>
            </dl>
          </section>

          <section class="community-detail__section">
            <h3>互动数据</h3>
            <div class="community-metric-grid">
              <div>
                <strong>{{ activePost.like_count }}</strong>
                <span>点赞</span>
              </div>
              <div>
                <strong>{{ activePost.comment_count }}</strong>
                <span>评论</span>
              </div>
              <div>
                <strong>{{ activePost.favorite_count }}</strong>
                <span>收藏</span>
              </div>
            </div>
          </section>

          <section class="community-detail__section">
            <h3>媒体</h3>
            <div v-if="activePost.media_assets.length === 0" class="community-empty-inline">
              暂无媒体
            </div>
            <div v-else class="community-asset-list">
              <article v-for="asset in activePost.media_assets" :key="asset.asset_id" class="community-asset">
                <div class="community-asset__title">
                  <span>{{ asset.file_name }}</span>
                  <el-tag size="small" type="info">{{ mediaTypeLabel(asset.media_type) }}</el-tag>
                </div>
                <div class="community-cell__meta">
                  {{ asset.content_type }} · {{ formatFileSize(asset.file_size) }}
                </div>
                <div class="community-cell__meta">
                  审核：{{ asset.review_status }} · 状态：{{ asset.upload_status }}
                </div>
              </article>
            </div>
          </section>

          <section class="community-detail__section">
            <el-button
              :type="activePost.review_status === 'rejected' ? 'primary' : 'danger'"
              :loading="processingPostId === activePost.post_id"
              @click="openStatusDialog(activePost)"
            >
              {{ activePost.review_status === 'rejected' ? '恢复帖子' : '下架帖子' }}
            </el-button>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="statusDialogVisible" :title="statusDialogTitle" width="560px">
      <div v-if="activeStatusPost" class="community-dialog-context">
        <div class="community-dialog-context__title">
          {{ activeStatusPost.title }} · #{{ activeStatusPost.post_id }}
        </div>
        <div class="community-dialog-context__meta">
          当前状态：{{ reviewStatusLabel(activeStatusPost.review_status) }} · 作者：{{ activeStatusPost.author.nickname || activeStatusPost.author.user_id }}
        </div>
      </div>
      <el-form label-position="top" class="community-form">
        <el-form-item label="治理动作">
          <el-tag :type="statusForm.action === 'take_down' ? 'danger' : 'success'">
            {{ governanceActionLabel(statusForm.action) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="statusForm.adminNotes"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说明本次下架或恢复依据，便于后续审计排查"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button
          :type="statusForm.action === 'take_down' ? 'danger' : 'primary'"
          :loading="statusSubmitting"
          @click="submitStatusForm"
        >
          {{ governanceActionLabel(statusForm.action) }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  getAdminCommunityPost,
  listAdminCommunityPosts,
  listCommunityAuditLogs,
  updateAdminCommunityPostStatus,
  type CommunityAuditActionFilter,
  type CommunityAuditLogSnapshot,
  type CommunityGovernanceAction,
  type CommunityMediaType,
  type CommunityPostSnapshot,
  type CommunityPostType,
  type CommunityPostTypeFilter,
  type CommunityReviewStatus,
  type CommunityReviewStatusFilter,
  type CommunityVisibility,
  type CommunityVisibilityFilter
} from '@/shared/api/adminCommunityApi';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

type ElementTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

const posts = ref<CommunityPostSnapshot[]>([]);
const auditLogs = ref<CommunityAuditLogSnapshot[]>([]);
const isLoading = ref(false);
const auditLogLoading = ref(false);
const detailLoading = ref(false);
const statusSubmitting = ref(false);
const errorMessage = ref('');
const auditLogErrorMessage = ref('');
const detailDrawerVisible = ref(false);
const statusDialogVisible = ref(false);
const processingPostId = ref<string | null>(null);
const activePost = ref<CommunityPostSnapshot | null>(null);
const activeStatusPost = ref<CommunityPostSnapshot | null>(null);

const filters = reactive<{
  postType: CommunityPostTypeFilter;
  reviewStatus: CommunityReviewStatusFilter;
  visibility: CommunityVisibilityFilter;
  authorUserId: string;
  topicId: string;
  keyword: string;
}>({
  postType: 'all',
  reviewStatus: 'all',
  visibility: 'all',
  authorUserId: '',
  topicId: '',
  keyword: ''
});

const auditLogFilters = reactive<{
  operatorId: string;
  action: CommunityAuditActionFilter;
}>({
  operatorId: '',
  action: 'all'
});

const statusForm = reactive<{
  action: CommunityGovernanceAction;
  adminNotes: string;
}>({
  action: 'take_down',
  adminNotes: ''
});

const approvedCount = computed(() => posts.value.filter((post) => post.review_status === 'approved').length);
const pendingCount = computed(() => posts.value.filter((post) => post.review_status === 'pending_review').length);
const rejectedCount = computed(() => posts.value.filter((post) => post.review_status === 'rejected').length);
const mediaPostCount = computed(() => posts.value.filter((post) => post.media_assets.length > 0).length);
const totalInteractions = computed(() =>
  posts.value.reduce((total, post) => total + post.like_count + post.comment_count + post.favorite_count, 0)
);
const statusDialogTitle = computed(() => `${governanceActionLabel(statusForm.action)}社区帖子`);
const summaryCards = computed(() => [
  {
    title: '当前帖子',
    description: '当前筛选条件下返回的帖子数量。',
    value: `${posts.value.length} 条`
  },
  {
    title: '待审核',
    description: '仍停留在待审核状态的帖子。',
    value: `${pendingCount.value} 条`
  },
  {
    title: '媒体帖子',
    description: '带有图片、视频或文件素材的帖子。',
    value: `${mediaPostCount.value} 条`
  },
  {
    title: '互动总量',
    description: '点赞、评论和收藏的合计值。',
    value: `${totalInteractions.value} 次`
  }
]);

onMounted(() => {
  void loadPage();
});

async function loadPage() {
  await Promise.all([loadPosts(), loadAuditLogs()]);
}

async function loadPosts() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    posts.value = await listAdminCommunityPosts({
      postType: filters.postType,
      reviewStatus: filters.reviewStatus,
      visibility: filters.visibility,
      authorUserId: filters.authorUserId,
      topicId: filters.topicId,
      keyword: filters.keyword
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '社区帖子加载失败';
  } finally {
    isLoading.value = false;
  }
}

async function loadAuditLogs() {
  auditLogLoading.value = true;
  auditLogErrorMessage.value = '';
  try {
    auditLogs.value = await listCommunityAuditLogs({
      targetType: 'community_post',
      operatorId: auditLogFilters.operatorId,
      action: auditLogFilters.action
    });
  } catch (error) {
    auditLogErrorMessage.value = error instanceof Error ? error.message : '帖子治理审计加载失败';
  } finally {
    auditLogLoading.value = false;
  }
}

async function openDetail(post: CommunityPostSnapshot) {
  detailDrawerVisible.value = true;
  activePost.value = post;
  detailLoading.value = true;
  try {
    activePost.value = await getAdminCommunityPost(post.post_id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '社区帖子详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openStatusDialog(post: CommunityPostSnapshot) {
  activeStatusPost.value = post;
  statusForm.action = post.review_status === 'rejected' ? 'restore' : 'take_down';
  statusForm.adminNotes = '';
  statusDialogVisible.value = true;
}

async function submitStatusForm() {
  if (!activeStatusPost.value) {
    return;
  }

  processingPostId.value = activeStatusPost.value.post_id;
  statusSubmitting.value = true;
  try {
    const updatedPost = await updateAdminCommunityPostStatus(
      activeStatusPost.value.post_id,
      statusForm.action,
      normalizeNullableText(statusForm.adminNotes)
    );
    posts.value = posts.value.map((post) => (post.post_id === updatedPost.post_id ? updatedPost : post));
    if (activePost.value?.post_id === updatedPost.post_id) {
      activePost.value = updatedPost;
    }
    statusDialogVisible.value = false;
    ElMessage.success(`${governanceActionLabel(statusForm.action)}已完成`);
    await loadAuditLogs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '帖子状态更新失败');
  } finally {
    statusSubmitting.value = false;
    processingPostId.value = null;
  }
}

function normalizeNullableText(value: string) {
  const normalizedValue = value.trim();
  return normalizedValue.length > 0 ? normalizedValue : null;
}

function governanceActionLabel(action: CommunityGovernanceAction) {
  const labelMap: Record<CommunityGovernanceAction, string> = {
    take_down: '下架',
    restore: '恢复'
  };
  return labelMap[action];
}

function postTypeLabel(postType: CommunityPostType) {
  const labelMap: Record<CommunityPostType, string> = {
    image_text: '图文',
    video: '视频',
    qa: '问答',
    experience: '经验'
  };
  return labelMap[postType];
}

function reviewStatusLabel(status: CommunityReviewStatus) {
  const labelMap: Record<CommunityReviewStatus, string> = {
    pending_review: '待审核',
    approved: '公开中',
    rejected: '已下架'
  };
  return labelMap[status];
}

function reviewStatusTagType(status: CommunityReviewStatus): ElementTagType {
  if (status === 'approved') {
    return 'success';
  }
  if (status === 'pending_review') {
    return 'warning';
  }
  return 'danger';
}

function visibilityLabel(visibility: CommunityVisibility) {
  const labelMap: Record<CommunityVisibility, string> = {
    public: '公开',
    follower: '粉丝可见'
  };
  return labelMap[visibility];
}

function petTypeLabel(petType: string) {
  const labelMap: Record<string, string> = {
    cat: '猫',
    dog: '狗'
  };
  return labelMap[petType] ?? petType;
}

function mediaTypeLabel(mediaType: CommunityMediaType) {
  const labelMap: Record<CommunityMediaType, string> = {
    image: '图片',
    video: '视频',
    file: '文件'
  };
  return labelMap[mediaType];
}

function auditActionLabel(action: string) {
  const labelMap: Record<string, string> = {
    community_post_take_down: '帖子下架',
    community_post_restore: '帖子恢复'
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

function formatFileSize(fileSize: number) {
  if (fileSize < 1024) {
    return `${fileSize} B`;
  }
  if (fileSize < 1024 * 1024) {
    return `${(fileSize / 1024).toFixed(1)} KB`;
  }
  return `${(fileSize / 1024 / 1024).toFixed(1)} MB`;
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
.community-summary,
.community-section {
  margin-top: 24px;
}

.community-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.community-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.community-filter {
  width: 132px;
}

.community-keyword {
  width: 210px;
}

.community-audit-filter {
  width: 160px;
}

.community-error {
  margin-bottom: 16px;
}

.community-table {
  border-radius: 20px;
  overflow: hidden;
  background: var(--pet-admin-surface);
}

.community-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.community-cell__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.community-cell__meta,
.community-cell__detail {
  color: var(--pet-admin-muted);
  font-size: 13px;
  line-height: 1.6;
}

.community-cell__detail {
  color: var(--pet-admin-body);
}

.community-count-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.community-count-stack strong {
  color: var(--pet-admin-title);
  font-size: 17px;
}

.community-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.community-audit-detail {
  max-height: 132px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--pet-admin-body);
  font-size: 12px;
  line-height: 1.6;
}

.community-detail {
  min-height: 320px;
}

.community-detail__section {
  padding: 18px 0;
  border-bottom: 1px solid var(--pet-admin-line);
}

.community-detail__section:first-child {
  padding-top: 0;
}

.community-detail__section h3 {
  margin: 0 0 12px;
  color: var(--pet-admin-title);
  font-size: 18px;
}

.community-detail__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.community-detail__heading h3 {
  margin: 0;
}

.community-detail__content {
  margin: 0 0 14px;
  color: var(--pet-admin-body);
  line-height: 1.8;
  white-space: pre-wrap;
}

.community-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.community-detail-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.community-detail-list div {
  display: grid;
  gap: 5px;
}

.community-detail-list dt {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.community-detail-list dd {
  margin: 0;
  color: var(--pet-admin-body);
  line-height: 1.7;
}

.community-metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.community-metric-grid div {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
}

.community-metric-grid strong {
  display: block;
  color: var(--pet-admin-title);
  font-size: 22px;
}

.community-metric-grid span {
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.community-empty-inline {
  padding: 16px;
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
  color: var(--pet-admin-muted);
  font-size: 13px;
}

.community-asset-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.community-asset {
  padding: 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface);
}

.community-asset__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--pet-admin-title);
  font-weight: 700;
}

.community-dialog-context {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--pet-admin-line);
  border-radius: 16px;
  background: var(--pet-admin-surface-soft);
}

.community-dialog-context__title {
  color: var(--pet-admin-title);
  font-size: 14px;
  font-weight: 700;
}

.community-dialog-context__meta {
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
  .community-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .community-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .community-filter,
  .community-keyword,
  .community-audit-filter {
    width: 100%;
  }

  .community-toolbar__actions,
  .community-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
