/**
 * GraphQL Subscriptions for Sync Jobs
 *
 * Subscriptions enable real-time updates via WebSocket
 */

import { gql } from '@apollo/client';

/**
 * Subscription to watch updates for a specific sync job
 * Useful for monitoring running jobs in real-time
 */
export const WATCH_SYNC_JOB = gql`
  subscription WatchSyncJob($id: ID!) {
    syncJobUpdated(id: $id) {
      id
      status
      recordsProcessed
      recordsFailed
      duration
      successRate
    }
  }
`;
