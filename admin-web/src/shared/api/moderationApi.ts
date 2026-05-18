import { adminRequest } from '@/shared/api/adminApi';

export type ModerationReportStatus = 'pending' | 'processed' | 'rejected';
export type ModerationReportListFilter = ModerationReportStatus | 'all';
export type ModerationProcessAction = 'confirm_violation' | 'dismiss_report';

export interface ModerationReportSnapshot {
  report_id: string;
  target_type: string;
  target_id: string;
  reason_code: string;
  reason_detail: string | null;
  status: ModerationReportStatus;
  processed_by: string | null;
  processed_at: string | null;
  created_at: string;
  reporter_user_id: string | null;
  reporter_nickname: string | null;
  reporter_mobile: string | null;
  post_id: string | null;
  post_title: string | null;
  post_content: string | null;
  post_review_status: string | null;
  post_visibility: string | null;
  post_deleted: boolean;
  post_author_user_id: string | null;
  post_author_nickname: string | null;
}

export function listModerationReports(status: ModerationReportListFilter = 'all') {
  const searchParams = new URLSearchParams();
  if (status !== 'all') {
    searchParams.set('status', status);
  }
  const queryString = searchParams.toString();
  return adminRequest<ModerationReportSnapshot[]>(
    `/api/v1/admin/moderation/reports${queryString ? `?${queryString}` : ''}`
  );
}

export function processModerationReport(
  reportId: string,
  action: ModerationProcessAction,
  adminNotes: string | null
) {
  return adminRequest<ModerationReportSnapshot>(`/api/v1/admin/moderation/reports/${reportId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      action,
      admin_notes: adminNotes
    })
  });
}
