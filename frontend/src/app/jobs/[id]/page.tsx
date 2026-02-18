'use client';

import { useParams } from 'next/navigation';
import Link from 'next/link';
import { useGraphQLSyncJob } from '@/hooks/useGraphQLSyncJob';
import SyncJobDetails from '@/components/SyncJobDetails';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';
import { SyncJob, SyncError } from '@/types/syncJob';

export default function JobDetailPage() {
  const params = useParams();
  const jobId = String(params.id);
  const { job, loading, error } = useGraphQLSyncJob(jobId);

  // Adapt GraphQL job to existing component interfaces
  const adaptedJob: SyncJob | null = job
    ? {
        id: Number(job.id),
        sourceName: job.sourceName,
        syncType: job.syncType,
        status: job.status,
        startTime: job.startTime,
        endTime: job.endTime,
        recordsProcessed: job.recordsProcessed,
        recordsFailed: job.recordsFailed,
        errorMessage: null,
        createdAt: job.startTime,
      }
    : null;

  const errors: SyncError[] = (job?.errors ?? []).map((e) => ({
    id: Number(e.id),
    errorType: e.errorType ?? '',
    errorMessage: e.errorMessage,
    failedRecord: e.failedRecord ?? null,
    occurredAt: e.occurredAt,
  }));

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Link href="/" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Dashboard
        </Link>

        {error && (
          <div className="mt-4">
            <ErrorAlert message={error.message} />
          </div>
        )}

        {loading ? (
          <LoadingSpinner />
        ) : adaptedJob ? (
          <>
            <h1 className="mt-4 text-2xl font-bold text-gray-900">
              Sync Job #{adaptedJob.id} — {adaptedJob.sourceName}
            </h1>
            <div className="mt-6">
              <SyncJobDetails job={adaptedJob} errors={errors} />
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
