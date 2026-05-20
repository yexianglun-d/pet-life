import { adminRequest } from '@/shared/api/adminApi';

export type PushTaskStatus = 'pending' | 'skipped' | 'failed' | 'sent';
export type PushTaskStatusFilter = PushTaskStatus | 'all';
export type PushDeliveryStatus = 'pending' | 'skipped' | 'failed' | 'sent';
export type PushDeliveryStatusFilter = PushDeliveryStatus | 'all';

export interface PushTaskSnapshot {
  push_task_id: string;
  user_id: string;
  notification_id: string | null;
  notify_type: string;
  biz_type: string | null;
  biz_id: string | null;
  title: string;
  content: string;
  provider_code: string;
  task_status: PushTaskStatus;
  failure_reason: string | null;
  created_at: string;
  updated_at: string;
}

export interface PushDeliveryRecordSnapshot {
  delivery_record_id: string;
  push_task_id: string;
  device_token_id: string;
  user_id: string;
  provider_code: string;
  delivery_status: PushDeliveryStatus;
  failure_reason: string | null;
  attempted_at: string | null;
  created_at: string;
}

export interface PushTaskListFilters {
  userId?: string;
  notificationId?: string;
  taskStatus?: PushTaskStatusFilter;
  providerCode?: string;
}

export interface PushDeliveryListFilters {
  pushTaskId?: string;
  userId?: string;
  deliveryStatus?: PushDeliveryStatusFilter;
  providerCode?: string;
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

export function listAdminPushTasks(filters: PushTaskListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'user_id', filters.userId);
  appendFilter(searchParams, 'notification_id', filters.notificationId);
  appendFilter(searchParams, 'task_status', filters.taskStatus);
  appendFilter(searchParams, 'provider_code', filters.providerCode);
  return adminRequest<PushTaskSnapshot[]>(`/api/v1/admin/push-tasks${resolveQueryString(searchParams)}`);
}

export function listAdminPushDeliveries(filters: PushDeliveryListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'push_task_id', filters.pushTaskId);
  appendFilter(searchParams, 'user_id', filters.userId);
  appendFilter(searchParams, 'delivery_status', filters.deliveryStatus);
  appendFilter(searchParams, 'provider_code', filters.providerCode);
  return adminRequest<PushDeliveryRecordSnapshot[]>(
    `/api/v1/admin/push-deliveries${resolveQueryString(searchParams)}`
  );
}
