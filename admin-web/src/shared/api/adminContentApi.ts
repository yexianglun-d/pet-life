import { adminRequest } from '@/shared/api/adminApi';

export type HealthRecordType = 'vaccine' | 'deworming' | 'examination' | 'medication' | 'weight' | 'observation';
export type HealthRecordTypeFilter = HealthRecordType | 'all';
export type DailyLogVisibility = 'private' | 'family' | 'public';
export type DailyLogVisibilityFilter = DailyLogVisibility | 'all';
export type BooleanFilter = 'all' | 'true' | 'false';
export type TimelineEventType = 'health' | 'daily_log' | 'service';
export type TimelineEventTypeFilter = TimelineEventType | 'all';
export type TimelineSourceType = 'health_record' | 'daily_log' | 'service_appointment';
export type TimelineSourceTypeFilter = TimelineSourceType | 'all';
export type TimelineSourceStatus = 'active' | 'deleted' | 'missing' | 'unsupported';
export type MediaType = 'image' | 'video' | 'file';

export interface MediaAssetSnapshot {
  asset_id: string;
  biz_type: string;
  media_type: MediaType;
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

export interface HealthRecordSnapshot {
  health_record_id: string;
  pet_id: string;
  record_type: HealthRecordType;
  title: string;
  value: string | null;
  unit: string | null;
  hospital_name: string | null;
  doctor_name: string | null;
  severity_level: string | null;
  result_summary: string | null;
  attachment_asset_ids: string[];
  attachment_assets: MediaAssetSnapshot[];
  next_reminder_id: string | null;
  next_reminder_at: string | null;
  next_reminder_status: string | null;
  occurred_at: string;
  notes: string | null;
  created_at: string;
}

export interface DailyLogSnapshot {
  daily_log_id: string;
  pet_id: string;
  content: string;
  media_asset_ids: string[];
  media_assets: MediaAssetSnapshot[];
  tags: string[];
  visibility: DailyLogVisibility;
  sync_to_community: boolean;
  community_post_id: string | null;
  happened_at: string;
  created_at: string;
}

export interface TimelineEventSnapshot {
  event_id: string;
  pet_id: string;
  event_type: TimelineEventType;
  source_type: TimelineSourceType;
  source_id: string;
  event_time: string;
  title: string;
  summary: string | null;
  cover_url: string | null;
  visibility: DailyLogVisibility;
  created_at: string;
}

export interface AdminPetContextSnapshot {
  pet_id: string;
  pet_name: string;
  pet_type: string;
  family_id: string | null;
  family_name: string | null;
  owner_user_id: string | null;
  owner_nickname: string | null;
  owner_mobile: string | null;
}

export interface AdminUserContextSnapshot {
  user_id: string;
  nickname: string | null;
  mobile: string | null;
}

export interface AdminHealthRecordSnapshot {
  health_record: HealthRecordSnapshot;
  pet: AdminPetContextSnapshot;
  operator: AdminUserContextSnapshot;
}

export interface AdminDailyLogSnapshot {
  daily_log: DailyLogSnapshot;
  pet: AdminPetContextSnapshot;
  author: AdminUserContextSnapshot;
}

export interface AdminTimelineEventSnapshot {
  timeline_event: TimelineEventSnapshot;
  pet: AdminPetContextSnapshot;
  source_status: TimelineSourceStatus;
}

export interface AdminHealthRecordListFilters {
  recordType?: HealthRecordTypeFilter;
  petId?: string;
  operatorUserId?: string;
  keyword?: string;
}

export interface AdminDailyLogListFilters {
  visibility?: DailyLogVisibilityFilter;
  syncToCommunity?: BooleanFilter;
  petId?: string;
  authorUserId?: string;
  keyword?: string;
}

export interface AdminTimelineEventListFilters {
  eventType?: TimelineEventTypeFilter;
  sourceType?: TimelineSourceTypeFilter;
  petId?: string;
  sourceId?: string;
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

export function listAdminHealthRecords(filters: AdminHealthRecordListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'record_type', filters.recordType);
  appendFilter(searchParams, 'pet_id', filters.petId);
  appendFilter(searchParams, 'operator_user_id', filters.operatorUserId);
  appendFilter(searchParams, 'keyword', filters.keyword);
  return adminRequest<AdminHealthRecordSnapshot[]>(
    `/api/v1/admin/health-records${resolveQueryString(searchParams)}`
  );
}

export function getAdminHealthRecord(healthRecordId: string) {
  return adminRequest<AdminHealthRecordSnapshot>(`/api/v1/admin/health-records/${healthRecordId}`);
}

export function listAdminDailyLogs(filters: AdminDailyLogListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'visibility', filters.visibility);
  if (filters.syncToCommunity === 'true') {
    searchParams.set('sync_to_community', 'true');
  }
  if (filters.syncToCommunity === 'false') {
    searchParams.set('sync_to_community', 'false');
  }
  appendFilter(searchParams, 'pet_id', filters.petId);
  appendFilter(searchParams, 'author_user_id', filters.authorUserId);
  appendFilter(searchParams, 'keyword', filters.keyword);
  return adminRequest<AdminDailyLogSnapshot[]>(
    `/api/v1/admin/daily-logs${resolveQueryString(searchParams)}`
  );
}

export function getAdminDailyLog(dailyLogId: string) {
  return adminRequest<AdminDailyLogSnapshot>(`/api/v1/admin/daily-logs/${dailyLogId}`);
}

export function listAdminTimelineEvents(filters: AdminTimelineEventListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'event_type', filters.eventType);
  appendFilter(searchParams, 'source_type', filters.sourceType);
  appendFilter(searchParams, 'pet_id', filters.petId);
  appendFilter(searchParams, 'source_id', filters.sourceId);
  return adminRequest<AdminTimelineEventSnapshot[]>(
    `/api/v1/admin/timeline/events${resolveQueryString(searchParams)}`
  );
}

export function getAdminTimelineEvent(eventId: string) {
  return adminRequest<AdminTimelineEventSnapshot>(`/api/v1/admin/timeline/events/${eventId}`);
}
