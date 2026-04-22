import { ADMIN_ACCESS_TOKEN_KEY, ADMIN_OPERATOR_NAME_KEY } from '@/shared/constants/adminSession';

interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
}

const DEFAULT_API_BASE_URL = 'http://127.0.0.1:8080';

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
