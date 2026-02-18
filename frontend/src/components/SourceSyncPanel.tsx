'use client';

import { useGraphQLTriggerSync } from '@/hooks/useGraphQLTriggerSync';

const sources = [
  { name: 'CRM', label: 'CRM Customers', color: 'bg-blue-600 hover:bg-blue-700' },
  { name: 'ERP', label: 'ERP Products', color: 'bg-emerald-600 hover:bg-emerald-700' },
  { name: 'ACCOUNTING', label: 'Accounting Invoices', color: 'bg-purple-600 hover:bg-purple-700' },
];

export default function SourceSyncPanel() {
  const { triggerSync, loading, error } = useGraphQLTriggerSync();

  return (
    <div className="flex items-center gap-2">
      {sources.map((source) => (
        <button
          key={source.name}
          onClick={() => triggerSync(source.name)}
          disabled={loading}
          className={`inline-flex items-center rounded-md px-3 py-2 text-sm font-medium text-white disabled:opacity-50 ${source.color}`}
        >
          {loading ? (
            <svg className="mr-1.5 h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          ) : null}
          {source.label}
        </button>
      ))}
      {error && <p className="text-sm text-red-600">{error.message}</p>}
    </div>
  );
}
