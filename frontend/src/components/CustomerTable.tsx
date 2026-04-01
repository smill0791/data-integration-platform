'use client';

import { Customer } from '@/types/customer';

const SOURCE_COLORS: Record<string, string> = {
  SALESFORCE: 'bg-sky-100 text-sky-700',
  CRM: 'bg-blue-100 text-blue-700',
  ERP: 'bg-purple-100 text-purple-700',
  ACCOUNTING: 'bg-green-100 text-green-700',
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleString();
}

export default function CustomerTable({ customers }: { customers: Customer[] }) {
  if (customers.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-gray-400">
        No synced customers found.
      </p>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            {['Source', 'Name', 'Email', 'Phone', 'Address', 'Last Synced'].map((h) => (
              <th
                key={h}
                className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500"
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {customers.map((c) => (
            <tr key={c.id} className="hover:bg-gray-50">
              <td className="px-4 py-3">
                <span
                  className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                    SOURCE_COLORS[c.sourceSystem] ?? 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {c.sourceSystem}
                </span>
              </td>
              <td className="px-4 py-3 font-medium text-gray-900">{c.name}</td>
              <td className="px-4 py-3 text-gray-600">{c.email ?? '—'}</td>
              <td className="px-4 py-3 text-gray-600">{c.phone ?? '—'}</td>
              <td className="px-4 py-3 text-gray-500">{c.address ?? '—'}</td>
              <td className="px-4 py-3 text-gray-500">{formatDate(c.lastSyncedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
