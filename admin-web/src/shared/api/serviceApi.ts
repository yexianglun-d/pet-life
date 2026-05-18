import { adminRequest } from '@/shared/api/adminApi';

export type ServiceProviderType = 'hospital' | 'boarding' | 'grooming' | 'training';
export type ServiceProviderStatus = 'online' | 'rest' | 'offline';
export type ProviderServiceItemStatus = 'active' | 'inactive';
export type ProviderScheduleSlotStatus = 'open' | 'closed' | 'full';
export type ServiceAppointmentStatus = 'pending_confirm' | 'confirmed' | 'completed' | 'canceled';
export type ProviderReviewStatus = 'visible' | 'hidden';
export type ServiceCityOpenedFilter = 'all' | 'opened' | 'closed';
export type ServiceAuditTargetType =
  | 'service_city'
  | 'service_provider'
  | 'provider_service_item'
  | 'provider_schedule_slot'
  | 'service_appointment'
  | 'provider_review';
export type ServiceAuditTargetTypeFilter = ServiceAuditTargetType | 'all';
export type ServiceListFilter<T extends string> = T | 'all';

export interface ServiceCityConfigSnapshot {
  config_id: string | null;
  city_code: string;
  city_name: string;
  opened: boolean;
  unavailable_reason: string | null;
  sort_order: number;
  created_at: string | null;
  updated_at: string | null;
}

export interface ProviderServiceItemSnapshot {
  service_item_id: string;
  service_code: string;
  service_name: string;
  service_desc: string | null;
  price_min: number | null;
  price_max: number | null;
  status: ProviderServiceItemStatus;
}

export interface ProviderScheduleSlotSnapshot {
  slot_id: string;
  provider_id: string;
  appointment_type: ServiceProviderType;
  slot_date: string;
  start_time: string;
  end_time: string;
  quota: number;
  booked_count: number;
  available_quota: number;
  status: ProviderScheduleSlotStatus;
  bookable: boolean;
}

export interface ServiceProviderSnapshot {
  provider_id: string;
  provider_type: ServiceProviderType;
  provider_name: string;
  city_code: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  contact_phone: string | null;
  business_hours: string | null;
  rating_avg: number | null;
  review_count: number;
  status: ServiceProviderStatus;
  bookable: boolean;
  service_items: ProviderServiceItemSnapshot[];
  available_slots: ProviderScheduleSlotSnapshot[];
  created_at: string;
  updated_at: string;
}

export interface ServiceAppointmentSnapshot {
  appointment_id: string;
  pet_id: string;
  pet_name: string;
  provider_id: string;
  provider_name: string;
  provider_type: ServiceProviderType;
  appointment_type: ServiceProviderType;
  appointment_date: string;
  appointment_slot: string;
  demand_desc: string | null;
  contact_name: string;
  contact_mobile: string;
  status: ServiceAppointmentStatus;
  remark: string | null;
  reviewed: boolean;
  created_at: string;
  updated_at: string;
}

export interface ProviderReviewSnapshot {
  review_id: string;
  provider_id: string;
  provider_name: string;
  provider_type: ServiceProviderType;
  appointment_id: string | null;
  user_id: string;
  reviewer_nickname: string;
  pet_id: string | null;
  pet_name: string | null;
  rating: number;
  content: string | null;
  status: ProviderReviewStatus;
  created_at: string;
  updated_at: string;
}

export interface ServiceAuditLogSnapshot {
  audit_log_id: string;
  operator_type: string;
  operator_id: string;
  target_type: ServiceAuditTargetType;
  target_id: string;
  action: string;
  detail_json: string;
  ip_address: string | null;
  user_agent: string | null;
  created_at: string;
}

export interface ServiceProviderListFilters {
  providerType?: ServiceListFilter<ServiceProviderType>;
  cityCode?: string;
  status?: ServiceListFilter<ServiceProviderStatus>;
}

export interface ServiceAppointmentListFilters {
  status?: ServiceListFilter<ServiceAppointmentStatus>;
  providerType?: ServiceListFilter<ServiceProviderType>;
  cityCode?: string;
}

export interface ProviderReviewListFilters {
  status?: ServiceListFilter<ProviderReviewStatus>;
  providerType?: ServiceListFilter<ServiceProviderType>;
  cityCode?: string;
}

export interface ServiceAuditLogListFilters {
  operatorId?: string;
  targetType?: ServiceAuditTargetTypeFilter;
  action?: string;
}

export interface ServiceCityConfigListFilters {
  cityCode?: string;
  opened?: ServiceCityOpenedFilter;
}

export interface UpsertServiceCityConfigPayload {
  city_code: string;
  city_name: string;
  opened: boolean;
  unavailable_reason: string | null;
  sort_order: number;
}

export interface UpsertServiceProviderPayload {
  provider_type: ServiceProviderType;
  provider_name: string;
  city_code: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  contact_phone: string | null;
  business_hours: string | null;
  rating_avg: number | null;
  review_count: number;
  status: ServiceProviderStatus;
}

export interface UpsertProviderServiceItemPayload {
  service_code: string;
  service_name: string;
  service_desc: string | null;
  price_min: number | null;
  price_max: number | null;
  status: ProviderServiceItemStatus;
}

export interface UpsertProviderScheduleSlotPayload {
  appointment_type: ServiceProviderType;
  slot_date: string;
  start_time: string;
  end_time: string;
  quota: number;
  status: ProviderScheduleSlotStatus;
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

export function listServiceProviders(filters: ServiceProviderListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'provider_type', filters.providerType);
  appendFilter(searchParams, 'city_code', filters.cityCode);
  appendFilter(searchParams, 'status', filters.status);
  return adminRequest<ServiceProviderSnapshot[]>(
    `/api/v1/admin/service/providers${resolveQueryString(searchParams)}`
  );
}

export function listServiceCityConfigs(filters: ServiceCityConfigListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'city_code', filters.cityCode);
  if (filters.opened === 'opened') {
    searchParams.set('opened', 'true');
  }
  if (filters.opened === 'closed') {
    searchParams.set('opened', 'false');
  }
  return adminRequest<ServiceCityConfigSnapshot[]>(
    `/api/v1/admin/service/cities${resolveQueryString(searchParams)}`
  );
}

export function upsertServiceCityConfig(payload: UpsertServiceCityConfigPayload) {
  return adminRequest<ServiceCityConfigSnapshot>('/api/v1/admin/service/cities', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function createServiceProvider(payload: UpsertServiceProviderPayload) {
  return adminRequest<ServiceProviderSnapshot>('/api/v1/admin/service/providers', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateServiceProvider(providerId: string, payload: UpsertServiceProviderPayload) {
  return adminRequest<ServiceProviderSnapshot>(`/api/v1/admin/service/providers/${providerId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  });
}

export function createProviderServiceItem(providerId: string, payload: UpsertProviderServiceItemPayload) {
  return adminRequest<ServiceProviderSnapshot>(`/api/v1/admin/service/providers/${providerId}/items`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateProviderServiceItem(
  providerId: string,
  serviceItemId: string,
  payload: UpsertProviderServiceItemPayload
) {
  return adminRequest<ServiceProviderSnapshot>(
    `/api/v1/admin/service/providers/${providerId}/items/${serviceItemId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }
  );
}

export function createProviderScheduleSlot(providerId: string, payload: UpsertProviderScheduleSlotPayload) {
  return adminRequest<ServiceProviderSnapshot>(`/api/v1/admin/service/providers/${providerId}/slots`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateProviderScheduleSlot(
  providerId: string,
  slotId: string,
  payload: UpsertProviderScheduleSlotPayload
) {
  return adminRequest<ServiceProviderSnapshot>(
    `/api/v1/admin/service/providers/${providerId}/slots/${slotId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }
  );
}

export function listServiceAppointments(filters: ServiceAppointmentListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'status', filters.status);
  appendFilter(searchParams, 'provider_type', filters.providerType);
  appendFilter(searchParams, 'city_code', filters.cityCode);
  return adminRequest<ServiceAppointmentSnapshot[]>(
    `/api/v1/admin/service/appointments${resolveQueryString(searchParams)}`
  );
}

export function listProviderReviews(filters: ProviderReviewListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'status', filters.status);
  appendFilter(searchParams, 'provider_type', filters.providerType);
  appendFilter(searchParams, 'city_code', filters.cityCode);
  return adminRequest<ProviderReviewSnapshot[]>(
    `/api/v1/admin/service/reviews${resolveQueryString(searchParams)}`
  );
}

export function listServiceAuditLogs(filters: ServiceAuditLogListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'operator_id', filters.operatorId);
  appendFilter(searchParams, 'target_type', filters.targetType);
  appendFilter(searchParams, 'action', filters.action);
  return adminRequest<ServiceAuditLogSnapshot[]>(
    `/api/v1/admin/service/audit-logs${resolveQueryString(searchParams)}`
  );
}

export function updateServiceAppointmentStatus(
  appointmentId: string,
  status: ServiceAppointmentStatus,
  remark: string | null
) {
  return adminRequest<ServiceAppointmentSnapshot>(`/api/v1/admin/service/appointments/${appointmentId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({
      status,
      remark
    })
  });
}

export function updateProviderReviewStatus(reviewId: string, status: ProviderReviewStatus) {
  return adminRequest<ProviderReviewSnapshot>(`/api/v1/admin/service/reviews/${reviewId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({
      status
    })
  });
}
