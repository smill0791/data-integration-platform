'use client';

import { useGraphQLDashboard } from '@/hooks/useGraphQLDashboard';
import MetricCard from '@/components/MetricCard';
import SyncJobTable from '@/components/SyncJobTable';
import SyncMetricsChart from '@/components/SyncMetricsChart';
import TriggerSyncButton from '@/components/TriggerSyncButton';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';
import { SyncJob } from '@/types/syncJob';

export default function DashboardPage() {
  const { jobs, metrics, loading, error } = useGraphQLDashboard();

  // Adapt GraphQL jobs to existing SyncJob interface for components
  const adaptedJobs: SyncJob[] = jobs.map((j) => ({
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
          <TriggerSyncButton />
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
              <h2 className="mb-3 text-lg font-medium text-gray-900">Recent Sync Jobs</h2>
              <SyncJobTable jobs={adaptedJobs} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
