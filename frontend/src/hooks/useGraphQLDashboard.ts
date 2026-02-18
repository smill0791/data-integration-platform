import { useQuery } from '@apollo/client';
import { GET_DASHBOARD_DATA } from '@/graphql/queries/syncJobs';
import { GraphQLSyncJob, SyncMetrics } from '@/types/syncJob';

interface DashboardData {
  syncJobs: GraphQLSyncJob[];
  syncMetrics: SyncMetrics;
}

export function useGraphQLDashboard() {
  const { data, loading, error } = useQuery<DashboardData>(GET_DASHBOARD_DATA, {
    pollInterval: 10000,
  });

  return {
    jobs: data?.syncJobs ?? [],
    metrics: data?.syncMetrics ?? null,
    loading,
    error,
  };
}
