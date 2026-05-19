import { adminRequest } from '@/shared/api/adminApi';

export type SmsScene = 'login';
export type SmsSceneFilter = SmsScene | 'all';
export type SmsVerificationStatus = 'active' | 'verified' | 'expired' | 'locked' | 'send_failed';
export type SmsVerificationStatusFilter = SmsVerificationStatus | 'all';
export type SmsSendStatus = 'accepted' | 'failed' | 'blocked';
export type SmsSendStatusFilter = SmsSendStatus | 'all';

export interface SmsVerificationRecordSnapshot {
  verification_id: string;
  mobile: string;
  scene: SmsScene;
  expires_at: string;
  verified_at: string | null;
  attempt_count: number;
  max_attempt_count: number;
  status: SmsVerificationStatus;
  request_ip: string | null;
  user_agent: string | null;
  created_at: string;
  updated_at: string;
}

export interface SmsSendRecordSnapshot {
  send_record_id: string;
  verification_id: string | null;
  mobile: string;
  scene: SmsScene;
  provider_code: string;
  send_status: SmsSendStatus;
  failure_reason: string | null;
  request_ip: string | null;
  user_agent: string | null;
  created_at: string;
}

export interface SmsVerificationListFilters {
  mobile?: string;
  scene?: SmsSceneFilter;
  status?: SmsVerificationStatusFilter;
}

export interface SmsSendRecordListFilters {
  mobile?: string;
  scene?: SmsSceneFilter;
  providerCode?: string;
  sendStatus?: SmsSendStatusFilter;
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

export function listAdminSmsVerificationRecords(filters: SmsVerificationListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'mobile', filters.mobile);
  appendFilter(searchParams, 'scene', filters.scene);
  appendFilter(searchParams, 'status', filters.status);
  return adminRequest<SmsVerificationRecordSnapshot[]>(
    `/api/v1/admin/sms-verifications${resolveQueryString(searchParams)}`
  );
}

export function listAdminSmsSendRecords(filters: SmsSendRecordListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'mobile', filters.mobile);
  appendFilter(searchParams, 'scene', filters.scene);
  appendFilter(searchParams, 'provider_code', filters.providerCode);
  appendFilter(searchParams, 'send_status', filters.sendStatus);
  return adminRequest<SmsSendRecordSnapshot[]>(
    `/api/v1/admin/sms-send-records${resolveQueryString(searchParams)}`
  );
}
