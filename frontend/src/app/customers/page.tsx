'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useCustomers } from '@/hooks/useCustomers';
import CustomerTable from '@/components/CustomerTable';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';

const SOURCE_FILTERS = [
  { value: undefined, label: 'All Sources' },
  { value: 'SALESFORCE', label: 'Salesforce' },
  { value: 'CRM', label: 'CRM' },
  { value: 'ERP', label: 'ERP' },
  { value: 'ACCOUNTING', label: 'Accounting' },
];

export default function CustomersPage() {
  const [source, setSource] = useState<string | undefined>(undefined);
  const { data: customers, isLoading, error } = useCustomers(source);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6 flex items-center gap-3">
          <Link href="/" className="text-sm text-blue-600 hover:underline">
            ← Back to Dashboard
          </Link>
        </div>

        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Synced Customers</h1>
            <p className="mt-1 text-sm text-gray-500">
              Records that have been synced into the final schema
            </p>
          </div>
          <span className="text-sm text-gray-400">
            {customers ? `${customers.length} records` : ''}
          </span>
        </div>

        <div className="mt-4 flex gap-1">
          {SOURCE_FILTERS.map((f) => (
            <button
              key={f.label}
              onClick={() => setSource(f.value)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                source === f.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        <div className="mt-4">
          {isLoading ? (
            <LoadingSpinner />
          ) : error ? (
            <ErrorAlert message={(error as Error).message} />
          ) : (
            <CustomerTable customers={customers ?? []} />
          )}
        </div>
      </div>
    </div>
  );
}
