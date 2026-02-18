export interface SyncJob {
  id: number;
  sourceName: string;
  syncType: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
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
