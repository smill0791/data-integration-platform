import { Customer } from '@/types/customer';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export function getCustomers(source?: string): Promise<Customer[]> {
  const url = new URL(`${API_BASE}/api/customers`);
  if (source) url.searchParams.set('source', source);
  return fetch(url.toString()).then((r) => {
    if (!r.ok) throw new Error(`API error: ${r.status}`);
    return r.json();
  });
}
