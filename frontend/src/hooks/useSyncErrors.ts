import { useQuery } from '@tanstack/react-query';
import { getJobErrors } from '@/services/syncJobService';
import { SyncError } from '@/types/syncJob';

export function useSyncErrors(jobId: number) {
  return useQuery<SyncError[]>({
    queryKey: ['syncErrors', jobId],
    queryFn: () => getJobErrors(jobId),
  });
}
