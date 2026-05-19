import { adminRequest } from '@/shared/api/adminApi';

export type CommunityPostType = 'image_text' | 'video' | 'qa' | 'experience';
export type CommunityPostTypeFilter = CommunityPostType | 'all';
export type CommunityReviewStatus = 'pending_review' | 'approved' | 'rejected';
export type CommunityReviewStatusFilter = CommunityReviewStatus | 'all';
export type CommunityVisibility = 'public' | 'follower';
export type CommunityVisibilityFilter = CommunityVisibility | 'all';
export type CommunityGovernanceAction = 'take_down' | 'restore';
export type CommunityAuditTargetType = 'moderation_report' | 'community_post' | 'community_question';
export type CommunityAuditTargetTypeFilter = CommunityAuditTargetType | 'all';
export type CommunityAuditAction =
  | 'moderation_report_confirm_violation'
  | 'moderation_report_dismiss_report'
  | 'community_post_take_down'
  | 'community_post_restore'
  | 'community_question_take_down'
  | 'community_question_restore';
export type CommunityAuditActionFilter = CommunityAuditAction | 'all';
export type CommunityMediaType = 'image' | 'video' | 'file';

export interface CommunityMediaAssetSnapshot {
  asset_id: string;
  biz_type: string;
  media_type: CommunityMediaType;
  file_name: string;
  content_type: string;
  file_size: number;
  file_hash: string;
  upload_status: string;
  review_status: string;
  access_url: string | null;
  completed_at: string | null;
  created_at: string;
}

export interface CommunityAuthorSnapshot {
  user_id: string;
  nickname: string | null;
  avatar_url: string | null;
}

export interface CommunityPetSnapshot {
  pet_id: string;
  pet_name: string;
  pet_type: string;
  breed: string | null;
}

export interface CommunityTopicSnapshot {
  topic_id: string;
  topic_name: string;
  topic_desc: string | null;
  city_code: string | null;
  status: string | null;
  created_at: string | null;
  updated_at: string | null;
}

export interface CommunityPostSnapshot {
  post_id: string;
  post_type: CommunityPostType;
  title: string;
  content: string;
  source_daily_log_id: string | null;
  topic: CommunityTopicSnapshot | null;
  media_asset_ids: string[];
  media_assets: CommunityMediaAssetSnapshot[];
  city_code: string | null;
  visibility: CommunityVisibility;
  review_status: CommunityReviewStatus;
  like_count: number;
  comment_count: number;
  favorite_count: number;
  liked: boolean;
  favorited: boolean;
  published_at: string;
  created_at: string;
  author: CommunityAuthorSnapshot;
  pet: CommunityPetSnapshot | null;
}

export interface CommunityCommentSnapshot {
  comment_id: string;
  post_id: string;
  content: string;
  created_at: string;
  author: CommunityAuthorSnapshot;
}

export interface CommunityQuestionDetailSnapshot {
  question: CommunityPostSnapshot;
  answers: CommunityCommentSnapshot[];
}

export interface CommunityAuditLogSnapshot {
  audit_log_id: string;
  operator_type: string;
  operator_id: string;
  target_type: CommunityAuditTargetType;
  target_id: string;
  action: string;
  detail_json: string;
  ip_address: string | null;
  user_agent: string | null;
  created_at: string;
}

export interface AdminCommunityPostListFilters {
  postType?: CommunityPostTypeFilter;
  reviewStatus?: CommunityReviewStatusFilter;
  visibility?: CommunityVisibilityFilter;
  authorUserId?: string;
  topicId?: string;
  keyword?: string;
}

export interface AdminCommunityQuestionListFilters {
  reviewStatus?: CommunityReviewStatusFilter;
  visibility?: CommunityVisibilityFilter;
  authorUserId?: string;
  topicId?: string;
  keyword?: string;
}

export interface CommunityAuditLogListFilters {
  operatorId?: string;
  targetType?: CommunityAuditTargetTypeFilter;
  action?: CommunityAuditActionFilter;
}

function appendFilter(searchParams: URLSearchParams, key: string, value: string | undefined) {
  const normalizedValue = value?.trim();
  if (normalizedValue && normalizedValue !== 'all') {
    searchParams.set(key, normalizedValue);
  }
}

function resolveQueryString(searchParams: URLSearchParams) {
  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export function listAdminCommunityPosts(filters: AdminCommunityPostListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'post_type', filters.postType);
  appendFilter(searchParams, 'review_status', filters.reviewStatus);
  appendFilter(searchParams, 'visibility', filters.visibility);
  appendFilter(searchParams, 'author_user_id', filters.authorUserId);
  appendFilter(searchParams, 'topic_id', filters.topicId);
  appendFilter(searchParams, 'keyword', filters.keyword);
  return adminRequest<CommunityPostSnapshot[]>(
    `/api/v1/admin/community/posts${resolveQueryString(searchParams)}`
  );
}

export function getAdminCommunityPost(postId: string) {
  return adminRequest<CommunityPostSnapshot>(`/api/v1/admin/community/posts/${postId}`);
}

export function updateAdminCommunityPostStatus(
  postId: string,
  action: CommunityGovernanceAction,
  adminNotes: string | null
) {
  return adminRequest<CommunityPostSnapshot>(`/api/v1/admin/community/posts/${postId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({
      action,
      admin_notes: adminNotes
    })
  });
}

export function listAdminCommunityQuestions(filters: AdminCommunityQuestionListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'review_status', filters.reviewStatus);
  appendFilter(searchParams, 'visibility', filters.visibility);
  appendFilter(searchParams, 'author_user_id', filters.authorUserId);
  appendFilter(searchParams, 'topic_id', filters.topicId);
  appendFilter(searchParams, 'keyword', filters.keyword);
  return adminRequest<CommunityPostSnapshot[]>(
    `/api/v1/admin/community/questions${resolveQueryString(searchParams)}`
  );
}

export function getAdminCommunityQuestion(questionId: string) {
  return adminRequest<CommunityQuestionDetailSnapshot>(
    `/api/v1/admin/community/questions/${questionId}`
  );
}

export function updateAdminCommunityQuestionStatus(
  questionId: string,
  action: CommunityGovernanceAction,
  adminNotes: string | null
) {
  return adminRequest<CommunityPostSnapshot>(`/api/v1/admin/community/questions/${questionId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({
      action,
      admin_notes: adminNotes
    })
  });
}

export function listCommunityAuditLogs(filters: CommunityAuditLogListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'operator_id', filters.operatorId);
  appendFilter(searchParams, 'target_type', filters.targetType);
  appendFilter(searchParams, 'action', filters.action);
  return adminRequest<CommunityAuditLogSnapshot[]>(
    `/api/v1/admin/moderation/audit-logs${resolveQueryString(searchParams)}`
  );
}
