import { useQuery, useSubscription } from '@apollo/client';
import { GET_SYNC_JOB } from '@/graphql/queries/syncJobs';
import { WATCH_SYNC_JOB } from '@/graphql/subscriptions/syncJobs';
import { GraphQLSyncJob } from '@/types/syncJob';

interface SyncJobData {
  syncJob: GraphQLSyncJob;
}

interface SubscriptionData {
  syncJobUpdated: GraphQLSyncJob;
}

export function useGraphQLSyncJob(id: string) {
  const { data: queryData, loading, error } = useQuery<SyncJobData>(GET_SYNC_JOB, {
    variables: { id },
  });

  const isRunning = queryData?.syncJob?.status === 'RUNNING';

  const { data: subData } = useSubscription<SubscriptionData>(WATCH_SYNC_JOB, {
    variables: { id },
    skip: !isRunning,
  });

  // Merge subscription data over query data for real-time updates
  const job = queryData?.syncJob
    ? {
        ...queryData.syncJob,
        ...(subData?.syncJobUpdated ?? {}),
      }
    : null;

  return { job, loading, error };
}
