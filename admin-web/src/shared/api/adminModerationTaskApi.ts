import { adminRequest } from '@/shared/api/adminApi';

export type ModerationTaskTargetType = 'community_post' | 'community_question';
export type ModerationTaskTargetTypeFilter = ModerationTaskTargetType | 'all';
export type ModerationTaskContentType = 'text' | 'image_text' | 'video' | 'qa';
export type ModerationTaskContentTypeFilter = ModerationTaskContentType | 'all';
export type ModerationTaskReviewStatus = 'pending' | 'approved' | 'rejected' | 'failed';
export type ModerationTaskReviewStatusFilter = ModerationTaskReviewStatus | 'all';
export type ModerationReviewAction = 'approve' | 'reject';
export type ModerationAuditTargetType =
  | 'moderation_report'
  | 'moderation_task'
  | 'community_post'
  | 'community_question';
export type ModerationAuditTargetTypeFilter = ModerationAuditTargetType | 'all';

export interface ModerationTaskSnapshot {
  task_id: string;
  target_type: ModerationTaskTargetType;
  target_id: string;
  content_type: ModerationTaskContentType;
  content_snapshot: string;
  provider_code: string;
  review_status: ModerationTaskReviewStatus;
  review_result: string | null;
  risk_labels: string | null;
  failure_reason: string | null;
  callback_payload: string | null;
  reviewed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface ModerationAuditLogSnapshot {
  audit_log_id: string;
  operator_type: string;
  operator_id: string;
  target_type: ModerationAuditTargetType;
  target_id: string;
  action: string;
  detail_json: string;
  ip_address: string | null;
  user_agent: string | null;
  created_at: string;
}

export interface AdminModerationTaskListFilters {
  targetType?: ModerationTaskTargetTypeFilter;
  targetId?: string;
  contentType?: ModerationTaskContentTypeFilter;
  reviewStatus?: ModerationTaskReviewStatusFilter;
  providerCode?: string;
}

export interface AdminReviewModerationTaskPayload {
  action: ModerationReviewAction;
  risk_labels: string[] | null;
  admin_notes: string | null;
}

export interface ModerationAuditLogListFilters {
  operatorId?: string;
  targetType?: ModerationAuditTargetTypeFilter;
  action?: string;
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

export function listAdminModerationTasks(filters: AdminModerationTaskListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'target_type', filters.targetType);
  appendFilter(searchParams, 'target_id', filters.targetId);
  appendFilter(searchParams, 'content_type', filters.contentType);
  appendFilter(searchParams, 'review_status', filters.reviewStatus);
  appendFilter(searchParams, 'provider_code', filters.providerCode);
  return adminRequest<ModerationTaskSnapshot[]>(
    `/api/v1/admin/moderation/tasks${resolveQueryString(searchParams)}`
  );
}

export function getAdminModerationTask(taskId: string) {
  return adminRequest<ModerationTaskSnapshot>(`/api/v1/admin/moderation/tasks/${taskId}`);
}

export function reviewAdminModerationTask(taskId: string, payload: AdminReviewModerationTaskPayload) {
  return adminRequest<ModerationTaskSnapshot>(`/api/v1/admin/moderation/tasks/${taskId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  });
}

export function listModerationAuditLogs(filters: ModerationAuditLogListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'operator_id', filters.operatorId);
  appendFilter(searchParams, 'target_type', filters.targetType);
  appendFilter(searchParams, 'action', filters.action);
  return adminRequest<ModerationAuditLogSnapshot[]>(
    `/api/v1/admin/moderation/audit-logs${resolveQueryString(searchParams)}`
  );
}
