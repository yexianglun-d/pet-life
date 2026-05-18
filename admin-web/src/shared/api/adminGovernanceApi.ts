import { adminRequest } from '@/shared/api/adminApi';

export type AdminBooleanFilter = 'all' | 'true' | 'false';
export type UserPrivacyLevel = 'normal' | 'private';
export type UserPrivacyLevelFilter = UserPrivacyLevel | 'all';
export type FamilyRole = 'owner' | 'admin' | 'member';
export type FamilyRoleFilter = FamilyRole | 'all';
export type FamilyStatus = 1 | 2;
export type FamilyStatusFilter = 'all' | '1' | '2';
export type PetType = 'cat' | 'dog' | 'other';
export type PetTypeFilter = PetType | 'all';
export type PetStatus = 'active' | 'memorial' | 'rehomed';
export type PetStatusFilter = PetStatus | 'all';
export type NeuterStatus = 'unknown' | 'completed' | 'pending';

export interface AdminUserContextSnapshot {
  user_id: string;
  nickname: string | null;
  mobile: string | null;
}

export interface AdminPetContextSnapshot {
  pet_id: string;
  pet_name: string;
  pet_type: PetType | string;
  family_id: string | null;
  family_name: string | null;
  owner_user_id: string | null;
  owner_nickname: string | null;
  owner_mobile: string | null;
}

export interface AdminUserSettingsSnapshot {
  current_pet_id: string | null;
  notification_enabled: boolean;
  privacy_level: UserPrivacyLevel;
}

export interface AdminUserFamilySnapshot {
  family_id: string;
  family_name: string;
  role: FamilyRole;
  member_count: number;
}

export interface AdminUserSnapshot {
  user_id: string;
  mobile: string;
  nickname: string | null;
  avatar_url: string | null;
  city_code: string | null;
  city_name: string | null;
  status: number;
  last_login_at: string | null;
  created_at: string;
  settings: AdminUserSettingsSnapshot;
  primary_family: AdminUserFamilySnapshot | null;
  current_pet: AdminPetContextSnapshot | null;
  pet_count: number;
}

export interface AdminFamilyMemberSnapshot {
  member_id: string;
  user_id: string;
  nickname: string | null;
  mobile: string | null;
  role: FamilyRole;
  invite_status: 'pending' | 'joined' | 'rejected';
  joined_at: string | null;
}

export interface AdminFamilyPetSnapshot {
  pet_id: string;
  pet_name: string;
  pet_type: PetType | string;
  breed: string | null;
  status: PetStatus;
  owner_user_id: string | null;
  owner_nickname: string | null;
  owner_mobile: string | null;
}

export interface AdminFamilySnapshot {
  family_id: string;
  family_name: string;
  owner: AdminUserContextSnapshot;
  status: FamilyStatus;
  created_at: string;
  updated_at: string;
  member_count: number;
  pet_count: number;
  members: AdminFamilyMemberSnapshot[];
  pets: AdminFamilyPetSnapshot[];
}

export interface PetSnapshot {
  pet_id: string;
  pet_name: string;
  pet_type: PetType | string;
  breed: string | null;
  gender: string | null;
  birthday: string | null;
  adopt_date: string | null;
  neuter_status: NeuterStatus | string;
  avatar_url: string | null;
  weight_kg: string | null;
  allergy_notes: string | null;
  medical_history: string | null;
  status: PetStatus;
  created_at: string;
  updated_at: string;
}

export interface AdminPetFamilySnapshot {
  family_id: string;
  family_name: string | null;
  status: FamilyStatus | null;
  member_count: number;
}

export interface AdminPetSnapshot {
  pet: PetSnapshot;
  owner: AdminUserContextSnapshot;
  family: AdminPetFamilySnapshot | null;
}

export interface AdminUserListFilters {
  keyword?: string;
  mobile?: string;
  nickname?: string;
  cityCode?: string;
  notificationEnabled?: AdminBooleanFilter;
  privacyLevel?: UserPrivacyLevelFilter;
}

export interface AdminFamilyListFilters {
  keyword?: string;
  familyName?: string;
  memberMobile?: string;
  memberRole?: FamilyRoleFilter;
  status?: FamilyStatusFilter;
}

export interface AdminPetListFilters {
  keyword?: string;
  petName?: string;
  petType?: PetTypeFilter;
  status?: PetStatusFilter;
  ownerMobile?: string;
  familyId?: string;
}

function appendFilter(searchParams: URLSearchParams, key: string, value: string | undefined) {
  const normalizedValue = value?.trim();
  if (normalizedValue && normalizedValue !== 'all') {
    searchParams.set(key, normalizedValue);
  }
}

function appendBooleanFilter(searchParams: URLSearchParams, key: string, value: AdminBooleanFilter | undefined) {
  if (value === 'true' || value === 'false') {
    searchParams.set(key, value);
  }
}

function resolveQueryString(searchParams: URLSearchParams) {
  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export function listAdminUsers(filters: AdminUserListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'mobile', filters.mobile);
  appendFilter(searchParams, 'nickname', filters.nickname);
  appendFilter(searchParams, 'city_code', filters.cityCode);
  appendBooleanFilter(searchParams, 'notification_enabled', filters.notificationEnabled);
  appendFilter(searchParams, 'privacy_level', filters.privacyLevel);
  return adminRequest<AdminUserSnapshot[]>(`/api/v1/admin/users${resolveQueryString(searchParams)}`);
}

export function getAdminUser(userId: string) {
  return adminRequest<AdminUserSnapshot>(`/api/v1/admin/users/${userId}`);
}

export function listAdminFamilies(filters: AdminFamilyListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'family_name', filters.familyName);
  appendFilter(searchParams, 'member_mobile', filters.memberMobile);
  appendFilter(searchParams, 'member_role', filters.memberRole);
  appendFilter(searchParams, 'status', filters.status);
  return adminRequest<AdminFamilySnapshot[]>(`/api/v1/admin/families${resolveQueryString(searchParams)}`);
}

export function getAdminFamily(familyId: string) {
  return adminRequest<AdminFamilySnapshot>(`/api/v1/admin/families/${familyId}`);
}

export function listAdminPets(filters: AdminPetListFilters = {}) {
  const searchParams = new URLSearchParams();
  appendFilter(searchParams, 'keyword', filters.keyword);
  appendFilter(searchParams, 'pet_name', filters.petName);
  appendFilter(searchParams, 'pet_type', filters.petType);
  appendFilter(searchParams, 'status', filters.status);
  appendFilter(searchParams, 'owner_mobile', filters.ownerMobile);
  appendFilter(searchParams, 'family_id', filters.familyId);
  return adminRequest<AdminPetSnapshot[]>(`/api/v1/admin/pets${resolveQueryString(searchParams)}`);
}

export function getAdminPet(petId: string) {
  return adminRequest<AdminPetSnapshot>(`/api/v1/admin/pets/${petId}`);
}
