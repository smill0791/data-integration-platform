import { useQuery } from '@tanstack/react-query';
import { getRecentJobs } from '@/services/syncJobService';
import { SyncJob } from '@/types/syncJob';

export function useSyncJobs() {
  return useQuery<SyncJob[]>({
    queryKey: ['syncJobs'],
    queryFn: getRecentJobs,
    refetchInterval: (query) => {
      const hasActive = query.state.data?.some((job) => job.status === 'RUNNING' || job.status === 'QUEUED') ?? false;
      return hasActive ? 5000 : false;
    },
  });
}
