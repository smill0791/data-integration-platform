'use client';

import Link from 'next/link';
import { SyncJob } from '@/types/syncJob';
import StatusBadge from './StatusBadge';

function formatDuration(start: string, end: string | null): string {
  if (!end) return '...';
  const ms = new Date(end).getTime() - new Date(start).getTime();
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

function successRate(job: SyncJob): string {
  const total = (job.recordsProcessed || 0) + (job.recordsFailed || 0);
  if (total === 0) return '-';
  return `${Math.round(((job.recordsProcessed || 0) / total) * 100)}%`;
}

export default function SyncJobTable({ jobs }: { jobs: SyncJob[] }) {
  if (jobs.length === 0) {
    return (
      <div className="rounded-lg bg-white p-8 text-center text-gray-500 shadow">
        No sync jobs found. Trigger a sync to get started.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg bg-white shadow">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Source</th>
            <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
            <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Records</th>
            <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Success Rate</th>
            <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Duration</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200 bg-white">
          {jobs.map((job) => (
            <tr key={job.id} className="hover:bg-gray-50">
              <td className="whitespace-nowrap px-6 py-4">
                <Link href={`/jobs/${job.id}`} className="text-blue-600 hover:underline">
                  {job.sourceName}
                </Link>
              </td>
              <td className="whitespace-nowrap px-6 py-4">
                <StatusBadge status={job.status} />
              </td>
              <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-700">
                {job.recordsProcessed ?? 0} / {(job.recordsProcessed ?? 0) + (job.recordsFailed ?? 0)}
              </td>
              <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-700">
                {successRate(job)}
              </td>
              <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-700">
                {formatDuration(job.startTime, job.endTime)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
