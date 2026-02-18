'use client';

import { useState } from 'react';
import { useGraphQLDashboard } from '@/hooks/useGraphQLDashboard';
import MetricCard from '@/components/MetricCard';
import SyncJobTable from '@/components/SyncJobTable';
import SyncMetricsChart from '@/components/SyncMetricsChart';
import SourceSyncPanel from '@/components/SourceSyncPanel';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';
import { SyncJob } from '@/types/syncJob';

const sourceFilters = [
  { value: null, label: 'All Sources' },
  { value: 'CRM', label: 'CRM' },
  { value: 'ERP', label: 'ERP' },
  { value: 'ACCOUNTING', label: 'Accounting' },
];

export default function DashboardPage() {
  const { jobs, metrics, loading, error } = useGraphQLDashboard();
  const [sourceFilter, setSourceFilter] = useState<string | null>(null);

  // Adapt GraphQL jobs to existing SyncJob interface for components
  const adaptedJobs: SyncJob[] = jobs
    .filter((j) => !sourceFilter || j.sourceName === sourceFilter)
    .map((j) => ({
      id: Number(j.id),
      sourceName: j.sourceName,
      syncType: j.syncType,
      status: j.status,
      startTime: j.startTime,
      endTime: j.endTime,
      recordsProcessed: j.recordsProcessed,
      recordsFailed: j.recordsFailed,
      errorMessage: null,
      createdAt: j.startTime,
    }));

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">Data Integration Dashboard</h1>
          <SourceSyncPanel />
        </div>

        {error && (
          <div className="mt-6">
            <ErrorAlert message={error.message} />
          </div>
        )}

        {loading && !jobs.length ? (
          <LoadingSpinner />
        ) : (
          <>
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <MetricCard
                title="Syncs (24h)"
                value={metrics?.last24Hours.totalSyncs ?? 0}
              />
              <MetricCard
                title="Success Rate (24h)"
                value={`${Math.round(metrics?.last24Hours.successRate ?? 0)}%`}
              />
              <MetricCard
                title="Records (30d)"
                value={(metrics?.last30Days.totalRecords ?? 0).toLocaleString()}
              />
            </div>

            <div className="mt-6">
              <SyncMetricsChart data={metrics?.last30Days.dailyStats ?? []} />
            </div>

            <div className="mt-6">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-medium text-gray-900">Recent Sync Jobs</h2>
                <div className="flex gap-1">
                  {sourceFilters.map((f) => (
                    <button
                      key={f.label}
                      onClick={() => setSourceFilter(f.value)}
                      className={`rounded-full px-3 py-1 text-xs font-medium ${
                        sourceFilter === f.value
                          ? 'bg-gray-900 text-white'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>
              <SyncJobTable jobs={adaptedJobs} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
