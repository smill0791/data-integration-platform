/**
 * GraphQL Queries for Sync Jobs
 */

import { gql } from '@apollo/client';

/**
 * Fragment for SyncJob core fields
 * Reusable across queries to ensure consistency
 */
export const SYNC_JOB_CORE_FIELDS = gql`
  fragment SyncJobCoreFields on SyncJob {
    id
    sourceName
    syncType
    status
    startTime
    endTime
    recordsProcessed
    recordsFailed
    duration
    successRate
  }
`;

/**
 * Query to fetch a single sync job by ID
 */
export const GET_SYNC_JOB = gql`
  ${SYNC_JOB_CORE_FIELDS}
  query GetSyncJob($id: ID!) {
    syncJob(id: $id) {
      ...SyncJobCoreFields
      errors(limit: 10) {
        id
        errorType
        errorMessage
        failedRecord
        occurredAt
      }
      validationStats {
        totalRecords
        passedValidation
        failedValidation
        topErrors {
          errorType
          count
        }
      }
    }
  }
`;

/**
 * Query to fetch multiple sync jobs with optional filtering
 */
export const GET_SYNC_JOBS = gql`
  ${SYNC_JOB_CORE_FIELDS}
  query GetSyncJobs(
    $limit: Int = 20
    $offset: Int = 0
    $filter: SyncJobFilter
    $orderBy: SyncJobOrderBy
  ) {
    syncJobs(limit: $limit, offset: $offset, filter: $filter, orderBy: $orderBy) {
      ...SyncJobCoreFields
      errors(limit: 3) {
        id
        errorMessage
        occurredAt
      }
    }
  }
`;

/**
 * Query for dashboard - fetches jobs and metrics in one request
 */
export const GET_DASHBOARD_DATA = gql`
  ${SYNC_JOB_CORE_FIELDS}
  query GetDashboardData {
    syncJobs(limit: 20, orderBy: START_TIME_DESC) {
      ...SyncJobCoreFields
      errors(limit: 3) {
        id
        errorMessage
        occurredAt
      }
    }

    syncMetrics(period: LAST_30_DAYS) {
      last24Hours {
        totalSyncs
        successRate
        avgDuration
        totalRecords
      }
      last30Days {
        totalSyncs
        successRate
        avgDuration
        totalRecords
        dailyStats {
          date
          syncsCompleted
          syncsFailed
        }
      }
    }
  }
`;

/**
 * Query to fetch only sync metrics
 */
export const GET_SYNC_METRICS = gql`
  query GetSyncMetrics($period: MetricsPeriod!) {
    syncMetrics(period: $period) {
      last24Hours {
        totalSyncs
        successRate
        avgDuration
        totalRecords
      }
      last30Days {
        successRate
        totalRecords
        dailyStats {
          date
          syncsCompleted
          syncsFailed
        }
      }
    }
  }
`;
