import { useMutation } from '@apollo/client';
import { TRIGGER_SYNC } from '@/graphql/mutations/syncJobs';
import { GET_DASHBOARD_DATA } from '@/graphql/queries/syncJobs';

export function useGraphQLTriggerSync() {
  const [triggerSync, { loading, error }] = useMutation(TRIGGER_SYNC, {
    variables: {
      input: { sourceName: 'CRM', syncType: 'FULL' },
    },
    refetchQueries: [{ query: GET_DASHBOARD_DATA }],
  });

  return { triggerSync, loading, error };
}
