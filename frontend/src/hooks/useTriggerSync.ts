import { useMutation, useQueryClient } from '@tanstack/react-query';
import { triggerCustomerSync } from '@/services/syncJobService';

export function useTriggerSync() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: triggerCustomerSync,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['syncJobs'] });
    },
  });
}
