export interface SyncJob {
  id: number;
  sourceName: string;
  syncType: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  startTime: string;
  endTime: string | null;
  recordsProcessed: number;
  recordsFailed: number;
  errorMessage: string | null;
  createdAt: string;
}

export interface SyncError {
  id: number;
  errorType: string;
  errorMessage: string;
  failedRecord: string | null;
  occurredAt: string;
}

export interface DashboardMetrics {
  syncs24h: number;
  successRate24h: number;
  totalRecords30d: number;
}

export interface DailyStatPoint {
  date: string;
  syncsCompleted: number;
  syncsFailed: number;
}

// GraphQL-specific types

export interface ValidationStats {
  totalRecords: number;
  passedValidation: number;
  failedValidation: number;
  topErrors: { errorType: string; count: number }[];
}

export interface StagingRecord {
  id: string;
  externalId: string | null;
  rawData: string;
  receivedAt: string;
}

export interface MetricsSummary {
  totalSyncs: number;
  successRate: number;
  avgDuration: number;
  totalRecords: number;
  dailyStats: DailyStatPoint[];
}

export interface SyncMetrics {
  last24Hours: MetricsSummary;
  last30Days: MetricsSummary;
}

export interface GraphQLSyncJob {
  id: string;
  sourceName: string;
  syncType: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  startTime: string;
  endTime: string | null;
  recordsProcessed: number;
  recordsFailed: number;
  duration: number | null;
  successRate: number | null;
  errors?: SyncError[];
  validationStats?: ValidationStats;
}
