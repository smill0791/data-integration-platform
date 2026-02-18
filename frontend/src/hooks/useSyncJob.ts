import { useQuery } from '@tanstack/react-query';
import { getJobById } from '@/services/syncJobService';
import { SyncJob } from '@/types/syncJob';

export function useSyncJob(id: number) {
  return useQuery<SyncJob>({
    queryKey: ['syncJob', id],
    queryFn: () => getJobById(id),
    refetchInterval: (query) => {
      return query.state.data?.status === 'RUNNING' ? 5000 : false;
    },
  });
}
