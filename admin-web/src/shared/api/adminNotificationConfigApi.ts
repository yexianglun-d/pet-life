import { adminRequest } from '@/shared/api/adminApi';

export type NotificationChannelType = 'inbox' | 'sms' | 'push';
export type NotificationChannelTypeFilter = NotificationChannelType | 'all';
export type NotificationEnabledFilter = 'all' | 'true' | 'false';
export type NotificationConfigStatus = 'draft' | 'ready' | 'disabled';
export type NotificationConfigStatusFilter = NotificationConfigStatus | 'all';
export type NotificationAuditTargetType = 'message_template' | 'notification_channel';
export type NotificationAuditTargetTypeFilter = NotificationAuditTargetType | 'all';

export interface MessageTemplateSnapshot {
  template_id: string;
  template_code: string;
  channel_type: NotificationChannelType;
  title_template: string | null;
  content_template: string;
  enabled: boolean;
  created_at: string;
  updated_at: string;
}

export interface NotificationChannelConfigSnapshot {
  channel_config_id: string;
  channel_type: NotificationChannelType;
  provider_code: string;
  provider_name: string;
  enabled: boolean;
  config_status: NotificationConfigStatus;
  remark: string | null;
  created_at: string;
  updated_at: string;
}

export interface NotificationAuditLogSnapshot {
  audit_log_id: string;
  operator_type: string;
  operator_id: string;
  target_type: NotificationAuditTargetType;
  target_id: string;
  action: string;
  detail_json: string;
  ip_address: string | null;
  user_agent: string | null;
  created_at: string;
}

export interface MessageTemplateListFilters {
  keyword?: string;
  templateCode?: string;
  channelType?: NotificationChannelTypeFilter;
  enabled?: NotificationEnabledFilter;
}

export interface NotificationChannelConfigListFilters {
  channelType?: NotificationChannelTypeFilter;
  enabled?: NotificationEnabledFilter;
  providerCode?: string;
  configStatus?: NotificationConfigStatusFilter;
}

export interface NotificationAuditLogListFilters {
  operatorId?: string;
  targetType?: NotificationAuditTargetTypeFilter;
  action?: string;
}

export interface UpsertMessageTemplatePayload {
  template_code: string;
  channel_type: NotificationChannelType;
  title_template: string | null;
  content_template: string;
  enabled: boolean;
}

export interface UpsertNotificationChannelPayload {
  channel_type: NotificationChannelType;
  provider_code: string;
  provider_name: string;
  enabled: boolean;
  config_status: NotificationConfigStatus;
  remark: string | null;
}

function appendFilter(searchParams: URLSearchParams, key: string, value: string | undefined) {
  const normalizedValue = value?.trim();
  if (normalizedValue && normalizedValue !== 'all') {
    searchParams.set(key, normalizedValue);
  }
}

function appendBooleanFilter(searchParams: URLSearchParams, key: string, value: NotificationEnabledFilter | undefined) {
  if (value === 'true' || value === 'false') {
    searchParams.set(key, value);
  }
}

function resolveQueryString(searchParams: URLSearchParams) {
  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export function listAdminMessageTemplates(filters: MessageTemplateListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'template_code', filters.templateCode);
  appendFilter(searchParams, 'channel_type', filters.channelType);
  appendBooleanFilter(searchParams, 'enabled', filters.enabled);
  return adminRequest<MessageTemplateSnapshot[]>(
    `/api/v1/admin/message-templates${resolveQueryString(searchParams)}`
  );
}

export function getAdminMessageTemplate(templateId: string) {
  return adminRequest<MessageTemplateSnapshot>(`/api/v1/admin/message-templates/${templateId}`);
}

export function createAdminMessageTemplate(payload: UpsertMessageTemplatePayload) {
  return adminRequest<MessageTemplateSnapshot>('/api/v1/admin/message-templates', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateAdminMessageTemplate(templateId: string, payload: UpsertMessageTemplatePayload) {
  return adminRequest<MessageTemplateSnapshot>(`/api/v1/admin/message-templates/${templateId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  });
}

export function updateAdminMessageTemplateStatus(templateId: string, enabled: boolean) {
  return adminRequest<MessageTemplateSnapshot>(`/api/v1/admin/message-templates/${templateId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled })
  });
}

export function listAdminNotificationChannels(filters: NotificationChannelConfigListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'channel_type', filters.channelType);
  appendBooleanFilter(searchParams, 'enabled', filters.enabled);
  appendFilter(searchParams, 'provider_code', filters.providerCode);
  appendFilter(searchParams, 'config_status', filters.configStatus);
  return adminRequest<NotificationChannelConfigSnapshot[]>(
    `/api/v1/admin/notification-channels${resolveQueryString(searchParams)}`
  );
}

export function getAdminNotificationChannel(channelConfigId: string) {
  return adminRequest<NotificationChannelConfigSnapshot>(
    `/api/v1/admin/notification-channels/${channelConfigId}`
  );
}

export function createAdminNotificationChannel(payload: UpsertNotificationChannelPayload) {
  return adminRequest<NotificationChannelConfigSnapshot>('/api/v1/admin/notification-channels', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateAdminNotificationChannel(
  channelConfigId: string,
  payload: UpsertNotificationChannelPayload
) {
  return adminRequest<NotificationChannelConfigSnapshot>(
    `/api/v1/admin/notification-channels/${channelConfigId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }
  );
}

export function updateAdminNotificationChannelStatus(channelConfigId: string, enabled: boolean) {
  return adminRequest<NotificationChannelConfigSnapshot>(
    `/api/v1/admin/notification-channels/${channelConfigId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ enabled })
    }
  );
}

export function listNotificationAuditLogs(filters: NotificationAuditLogListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'operator_id', filters.operatorId);
  appendFilter(searchParams, 'target_type', filters.targetType);
  appendFilter(searchParams, 'action', filters.action);
  return adminRequest<NotificationAuditLogSnapshot[]>(
    `/api/v1/admin/notification/audit-logs${resolveQueryString(searchParams)}`
  );
}
