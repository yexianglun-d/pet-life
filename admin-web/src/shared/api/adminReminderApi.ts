import { adminRequest } from '@/shared/api/adminApi';

export type ReminderType = 'vaccine' | 'deworming' | 'examination' | 'medication' | 'custom';
export type ReminderTypeFilter = ReminderType | 'all';
export type ReminderStatus = 'pending' | 'completed' | 'skipped';
export type ReminderStatusFilter = ReminderStatus | 'all';
export type ReminderMode = 'single' | 'cycle';
export type ReminderModeFilter = ReminderMode | 'all';
export type ReminderCycleUnit = 'day' | 'week' | 'month';
export type ReminderPetType = 'all' | 'cat' | 'dog' | 'other';
export type ReminderPetTypeFilter = ReminderPetType | 'all_pet';
export type ReminderEnabledFilter = 'all' | 'true' | 'false';
export type HealthReminderSourceType = 'vaccine' | 'deworming' | 'examination' | 'medication' | 'weight' | 'observation';
export type AdminReminderSourceStatus = 'active' | 'deleted' | 'missing';

export interface ReminderSnapshot {
  reminder_id: string;
  pet_id: string;
  reminder_type: ReminderType;
  title: string;
  reminder_mode: ReminderMode;
  cycle_value: number | null;
  cycle_unit: ReminderCycleUnit | null;
  due_at: string;
  status: ReminderStatus;
  notes: string | null;
  completed_at: string | null;
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

export interface AdminReminderSourceSnapshot {
  source_record_id: string;
  record_type: HealthReminderSourceType | null;
  title: string | null;
  status: AdminReminderSourceStatus;
}

export interface AdminReminderSnapshot {
  reminder: ReminderSnapshot;
  pet: AdminPetContextSnapshot;
  handler: AdminUserContextSnapshot | null;
  source_record: AdminReminderSourceSnapshot | null;
}

export interface ReminderTemplateSnapshot {
  template_id: string;
  template_name: string;
  reminder_type: ReminderType;
  default_reminder_mode: ReminderMode;
  default_advance_value: number;
  default_advance_unit: ReminderCycleUnit;
  default_cycle_value: number | null;
  default_cycle_unit: ReminderCycleUnit | null;
  applicable_pet_type: ReminderPetType;
  enabled: boolean;
  sort_order: number;
  created_at: string;
  updated_at: string;
}

export interface AdminReminderListFilters {
  keyword?: string;
  status?: ReminderStatusFilter;
  reminderType?: ReminderTypeFilter;
  reminderMode?: ReminderModeFilter;
  petId?: string;
  familyId?: string;
  ownerUserId?: string;
  handlerUserId?: string;
  sourceRecordId?: string;
  dueFrom?: string;
  dueTo?: string;
}

export interface AdminReminderTemplateListFilters {
  keyword?: string;
  reminderType?: ReminderTypeFilter;
  defaultReminderMode?: ReminderModeFilter;
  applicablePetType?: ReminderPetTypeFilter;
  enabled?: ReminderEnabledFilter;
}

export interface UpsertReminderTemplatePayload {
  template_name: string;
  reminder_type: ReminderType;
  default_reminder_mode: ReminderMode;
  default_advance_value: number;
  default_advance_unit: ReminderCycleUnit;
  default_cycle_value: number | null;
  default_cycle_unit: ReminderCycleUnit | null;
  applicable_pet_type: ReminderPetType;
  enabled: boolean;
  sort_order: number;
}

function appendFilter(searchParams: URLSearchParams, key: string, value: string | undefined) {
  const normalizedValue = value?.trim();
  if (normalizedValue && normalizedValue !== 'all' && normalizedValue !== 'all_pet') {
    searchParams.set(key, normalizedValue);
  }
}

function appendBooleanFilter(searchParams: URLSearchParams, key: string, value: ReminderEnabledFilter | undefined) {
  if (value === 'true' || value === 'false') {
    searchParams.set(key, value);
  }
}

function appendPetTypeFilter(searchParams: URLSearchParams, key: string, value: ReminderPetTypeFilter | undefined) {
  if (value && value !== 'all_pet') {
    searchParams.set(key, value);
  }
}

function resolveQueryString(searchParams: URLSearchParams) {
  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export function listAdminReminders(filters: AdminReminderListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'status', filters.status);
  appendFilter(searchParams, 'reminder_type', filters.reminderType);
  appendFilter(searchParams, 'reminder_mode', filters.reminderMode);
  appendFilter(searchParams, 'pet_id', filters.petId);
  appendFilter(searchParams, 'family_id', filters.familyId);
  appendFilter(searchParams, 'owner_user_id', filters.ownerUserId);
  appendFilter(searchParams, 'handler_user_id', filters.handlerUserId);
  appendFilter(searchParams, 'source_record_id', filters.sourceRecordId);
  appendFilter(searchParams, 'due_from', filters.dueFrom);
  appendFilter(searchParams, 'due_to', filters.dueTo);
  return adminRequest<AdminReminderSnapshot[]>(`/api/v1/admin/reminders${resolveQueryString(searchParams)}`);
}

export function getAdminReminder(reminderId: string) {
  return adminRequest<AdminReminderSnapshot>(`/api/v1/admin/reminders/${reminderId}`);
}

export function listAdminReminderTemplates(filters: AdminReminderTemplateListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'reminder_type', filters.reminderType);
  appendFilter(searchParams, 'default_reminder_mode', filters.defaultReminderMode);
  appendPetTypeFilter(searchParams, 'applicable_pet_type', filters.applicablePetType);
  appendBooleanFilter(searchParams, 'enabled', filters.enabled);
  return adminRequest<ReminderTemplateSnapshot[]>(
    `/api/v1/admin/reminder-templates${resolveQueryString(searchParams)}`
  );
}

export function getAdminReminderTemplate(templateId: string) {
  return adminRequest<ReminderTemplateSnapshot>(`/api/v1/admin/reminder-templates/${templateId}`);
}

export function createAdminReminderTemplate(payload: UpsertReminderTemplatePayload) {
  return adminRequest<ReminderTemplateSnapshot>('/api/v1/admin/reminder-templates', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateAdminReminderTemplate(templateId: string, payload: UpsertReminderTemplatePayload) {
  return adminRequest<ReminderTemplateSnapshot>(`/api/v1/admin/reminder-templates/${templateId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  });
}

export function updateAdminReminderTemplateStatus(templateId: string, enabled: boolean) {
  return adminRequest<ReminderTemplateSnapshot>(`/api/v1/admin/reminder-templates/${templateId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled })
  });
}
