import { useMutation } from '@apollo/client';
import { TRIGGER_SYNC } from '@/graphql/mutations/syncJobs';
import { GET_DASHBOARD_DATA } from '@/graphql/queries/syncJobs';

export function useGraphQLTriggerSync() {
  const [mutate, { loading, error }] = useMutation(TRIGGER_SYNC, {
    refetchQueries: [{ query: GET_DASHBOARD_DATA }],
  });

  const triggerSync = (sourceName: string = 'CRM') =>
    mutate({ variables: { input: { sourceName, syncType: 'FULL' } } });

  return { triggerSync, loading, error };
}
