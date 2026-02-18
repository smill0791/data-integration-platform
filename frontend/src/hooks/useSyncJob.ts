import { useQuery } from '@tanstack/react-query';
import { getJobById } from '@/services/syncJobService';
import { SyncJob } from '@/types/syncJob';

export function useSyncJob(id: number) {
  return useQuery<SyncJob>({
    queryKey: ['syncJob', id],
    queryFn: () => getJobById(id),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'RUNNING' || status === 'QUEUED' ? 5000 : false;
    },
  });
}
