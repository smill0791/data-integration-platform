'use client';

import { useSyncJobs } from '@/hooks/useSyncJobs';
import { useDashboardMetrics } from '@/hooks/useDashboardMetrics';
import MetricCard from '@/components/MetricCard';
import SyncJobTable from '@/components/SyncJobTable';
import SyncMetricsChart from '@/components/SyncMetricsChart';
import TriggerSyncButton from '@/components/TriggerSyncButton';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';

export default function DashboardPage() {
  const { data: jobs, isLoading, error } = useSyncJobs();
  const { metrics, dailyStats } = useDashboardMetrics(jobs);

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

        {isLoading ? (
          <LoadingSpinner />
        ) : (
          <>
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <MetricCard title="Syncs (24h)" value={metrics.syncs24h} />
              <MetricCard
                title="Success Rate (24h)"
                value={`${metrics.successRate24h}%`}
              />
              <MetricCard title="Records (30d)" value={metrics.totalRecords30d.toLocaleString()} />
            </div>

            <div className="mt-6">
              <SyncMetricsChart data={dailyStats} />
            </div>

            <div className="mt-6">
              <h2 className="mb-3 text-lg font-medium text-gray-900">Recent Sync Jobs</h2>
              <SyncJobTable jobs={jobs || []} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
