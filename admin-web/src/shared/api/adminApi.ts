import {
  ADMIN_ACCESS_TOKEN_KEY,
  ADMIN_OPERATOR_NAME_KEY,
  ADMIN_REFRESH_TOKEN_KEY,
  ADMIN_ROLE_CODE_KEY
} from '@/shared/constants/adminSession';

interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
}

const PRODUCTION_API_BASE_URL = 'https://pet.api.howied.me';
const LOCAL_API_BASE_URL = 'http://127.0.0.1:8080';
const DEFAULT_API_BASE_URL = import.meta.env.PROD ? PRODUCTION_API_BASE_URL : LOCAL_API_BASE_URL;

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  return (configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : DEFAULT_API_BASE_URL)
    .replace(/\/+$/, '');
}

export async function adminRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const accessToken = window.localStorage.getItem(ADMIN_ACCESS_TOKEN_KEY);
  const operatorName = window.localStorage.getItem(ADMIN_OPERATOR_NAME_KEY);
  const headers = new Headers(init?.headers);

  headers.set('Accept', 'application/json');
  headers.set('Content-Type', 'application/json');
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  if (operatorName) {
    headers.set('X-Admin-Operator', operatorName);
  }

  const response = await fetch(`${resolveApiBaseUrl()}${path}`, {
    ...init,
    headers
  });

  const payloadText = await response.text();
  if (!payloadText.trim()) {
    throw new Error('后台接口返回为空');
  }

  const payload = JSON.parse(payloadText) as ApiEnvelope<T>;
  if (response.status < 200 || response.status >= 300 || payload.code !== 'OK') {
    throw new Error(payload.message || '后台接口请求失败');
  }

  return payload.data;
}

export interface AdminAccountSnapshot {
  admin_account_id: string;
  username: string;
  display_name: string;
  role_code: string;
  status: number;
  last_login_at: string | null;
  created_at: string;
}

interface AdminLoginResponse {
  access_token: string;
  refresh_token: string;
  admin: AdminAccountSnapshot;
}

interface AdminRefreshResponse {
  access_token: string;
  refresh_token: string;
}

async function rawAdminRequest<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${resolveApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(init.headers ?? {})
    }
  });
  const payload = JSON.parse(await response.text()) as ApiEnvelope<T>;
  if (response.status < 200 || response.status >= 300 || payload.code !== 'OK') {
    throw new Error(payload.message || '后台接口请求失败');
  }
  return payload.data;
}

export async function loginAdmin(username: string, password: string) {
  const data = await rawAdminRequest<AdminLoginResponse>('/api/v1/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      username,
      password
    })
  });
  persistAdminSession(data);
  return data.admin;
}

export async function refreshAdminSession() {
  const refreshToken = window.localStorage.getItem(ADMIN_REFRESH_TOKEN_KEY);
  if (!refreshToken) {
    throw new Error('后台登录状态已失效');
  }
  const data = await rawAdminRequest<AdminRefreshResponse>('/api/v1/admin/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({
      refresh_token: refreshToken
    })
  });
  window.localStorage.setItem(ADMIN_ACCESS_TOKEN_KEY, data.access_token);
  window.localStorage.setItem(ADMIN_REFRESH_TOKEN_KEY, data.refresh_token);
}

export async function logoutAdmin() {
  const refreshToken = window.localStorage.getItem(ADMIN_REFRESH_TOKEN_KEY);
  if (refreshToken) {
    await rawAdminRequest<void>('/api/v1/admin/auth/logout', {
      method: 'POST',
      body: JSON.stringify({
        refresh_token: refreshToken
      })
    });
  }
  clearAdminSession();
}

export function clearAdminSession() {
  window.localStorage.removeItem(ADMIN_ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(ADMIN_REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(ADMIN_OPERATOR_NAME_KEY);
  window.localStorage.removeItem(ADMIN_ROLE_CODE_KEY);
}

function persistAdminSession(data: AdminLoginResponse) {
  window.localStorage.setItem(ADMIN_ACCESS_TOKEN_KEY, data.access_token);
  window.localStorage.setItem(ADMIN_REFRESH_TOKEN_KEY, data.refresh_token);
  window.localStorage.setItem(ADMIN_OPERATOR_NAME_KEY, data.admin.username);
  window.localStorage.setItem(ADMIN_ROLE_CODE_KEY, data.admin.role_code);
}
