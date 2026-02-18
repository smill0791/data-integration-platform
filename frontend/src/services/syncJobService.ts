import { SyncJob, SyncError } from '@/types/syncJob';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

export function getRecentJobs(): Promise<SyncJob[]> {
  return fetchJson<SyncJob[]>(`${API_BASE}/api/integrations/jobs`);
}

export function getJobById(id: number): Promise<SyncJob> {
  return fetchJson<SyncJob>(`${API_BASE}/api/integrations/jobs/${id}`);
}

export function getJobErrors(id: number): Promise<SyncError[]> {
  return fetchJson<SyncError[]>(`${API_BASE}/api/integrations/jobs/${id}/errors`);
}

export function triggerCustomerSync(): Promise<SyncJob> {
  return fetchJson<SyncJob>(`${API_BASE}/api/integrations/sync/customers`, {
    method: 'POST',
  });
}
