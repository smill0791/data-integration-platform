import { useQuery } from '@tanstack/react-query';
import { getCustomers } from '@/services/customerService';

export function useCustomers(source?: string) {
  return useQuery({
    queryKey: ['customers', source ?? 'all'],
    queryFn: () => getCustomers(source),
  });
}
