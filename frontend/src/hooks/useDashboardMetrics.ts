import { useMemo } from 'react';
import { SyncJob, DashboardMetrics, DailyStatPoint } from '@/types/syncJob';

export function useDashboardMetrics(jobs: SyncJob[] | undefined) {
  const metrics = useMemo<DashboardMetrics>(() => {
    if (!jobs || jobs.length === 0) {
      return { syncs24h: 0, successRate24h: 0, totalRecords30d: 0 };
    }

    const now = new Date();
    const twentyFourHoursAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    const jobs24h = jobs.filter(
      (j) => new Date(j.startTime) >= twentyFourHoursAgo
    );
    const completed24h = jobs24h.filter((j) => j.status === 'COMPLETED').length;
    const syncs24h = jobs24h.length;
    const successRate24h = syncs24h > 0 ? Math.round((completed24h / syncs24h) * 100) : 0;

    const totalRecords30d = jobs
      .filter((j) => new Date(j.startTime) >= thirtyDaysAgo)
      .reduce((sum, j) => sum + (j.recordsProcessed || 0), 0);

    return { syncs24h, successRate24h, totalRecords30d };
  }, [jobs]);

  const dailyStats = useMemo<DailyStatPoint[]>(() => {
    if (!jobs || jobs.length === 0) return [];

    const statsMap = new Map<string, { completed: number; failed: number }>();

    for (const job of jobs) {
      const date = new Date(job.startTime).toISOString().slice(0, 10);
      const entry = statsMap.get(date) || { completed: 0, failed: 0 };
      if (job.status === 'COMPLETED') {
        entry.completed++;
      } else if (job.status === 'FAILED') {
        entry.failed++;
      }
      statsMap.set(date, entry);
    }

    return Array.from(statsMap.entries())
      .map(([date, stats]) => ({
        date,
        syncsCompleted: stats.completed,
        syncsFailed: stats.failed,
      }))
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [jobs]);

  return { metrics, dailyStats };
}
