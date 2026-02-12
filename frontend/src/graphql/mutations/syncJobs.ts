/**
 * GraphQL Mutations for Sync Jobs
 */

import { gql } from '@apollo/client';
import { SYNC_JOB_CORE_FIELDS } from '../queries/syncJobs';

/**
 * Mutation to trigger a new sync job
 */
export const TRIGGER_SYNC = gql`
  ${SYNC_JOB_CORE_FIELDS}
  mutation TriggerSync($input: TriggerSyncInput!) {
    triggerSync(input: $input) {
      ...SyncJobCoreFields
    }
  }
`;

/**
 * Mutation to cancel a running sync job
 */
export const CANCEL_SYNC = gql`
  ${SYNC_JOB_CORE_FIELDS}
  mutation CancelSync($jobId: ID!) {
    cancelSync(jobId: $jobId) {
      ...SyncJobCoreFields
    }
  }
`;
