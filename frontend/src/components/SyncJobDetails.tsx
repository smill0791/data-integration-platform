'use client';

import { SyncJob, SyncError } from '@/types/syncJob';
import StatusBadge from './StatusBadge';

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString();
}

export default function SyncJobDetails({
  job,
  errors,
}: {
  job: SyncJob;
  errors: SyncError[];
}) {
  return (
    <div className="space-y-6">
      <div className="rounded-lg bg-white p-6 shadow">
        <h3 className="mb-4 text-lg font-medium text-gray-900">Job Summary</h3>
        <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          <div>
            <dt className="text-sm font-medium text-gray-500">Source</dt>
            <dd className="mt-1 text-sm text-gray-900">{job.sourceName}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Type</dt>
            <dd className="mt-1 text-sm text-gray-900">{job.syncType}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Status</dt>
            <dd className="mt-1"><StatusBadge status={job.status} /></dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Started</dt>
            <dd className="mt-1 text-sm text-gray-900">{formatDateTime(job.startTime)}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Ended</dt>
            <dd className="mt-1 text-sm text-gray-900">{formatDateTime(job.endTime)}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Records Processed</dt>
            <dd className="mt-1 text-sm text-gray-900">{job.recordsProcessed ?? 0}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-gray-500">Records Failed</dt>
            <dd className="mt-1 text-sm text-gray-900">{job.recordsFailed ?? 0}</dd>
          </div>
          {job.errorMessage && (
            <div className="col-span-2 sm:col-span-3">
              <dt className="text-sm font-medium text-gray-500">Error Message</dt>
              <dd className="mt-1 text-sm text-red-600">{job.errorMessage}</dd>
            </div>
          )}
        </dl>
      </div>

      {errors.length > 0 && (
        <div className="rounded-lg bg-white p-6 shadow">
          <h3 className="mb-4 text-lg font-medium text-gray-900">
            Error Log ({errors.length})
          </h3>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Message</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Failed Record</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {errors.map((error) => (
                  <tr key={error.id}>
                    <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-red-600">
                      {error.errorType}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {error.errorMessage}
                    </td>
                    <td className="max-w-xs truncate px-4 py-3 text-sm text-gray-500">
                      {error.failedRecord || '-'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-500">
                      {formatDateTime(error.occurredAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
