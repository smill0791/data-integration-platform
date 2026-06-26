'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useGraphQLSyncJob } from '@/hooks/useGraphQLSyncJob';
import { retryFailedRecords } from '@/services/syncJobService';
import SyncJobDetails from '@/components/SyncJobDetails';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';
import { SyncJob, SyncError } from '@/types/syncJob';

export default function JobDetailPage() {
  const params = useParams();
  const router = useRouter();
  const jobId = String(params.id);
  const { job, loading, error } = useGraphQLSyncJob(jobId);
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);

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

  // Retry is only wired for Salesforce contacts and only meaningful when there
  // are failed records carrying an external ID to re-fetch.
  const canRetry =
    adaptedJob?.sourceName === 'SALESFORCE' &&
    errors.some((e) => e.failedRecord);

  const handleRetry = async () => {
    if (!adaptedJob) return;
    setRetrying(true);
    setRetryError(null);
    try {
      const newJob = await retryFailedRecords(adaptedJob.id);
      if (newJob) {
        router.push(`/jobs/${newJob.id}`);
      } else {
        setRetryError('No failed records to retry.');
        setRetrying(false);
      }
    } catch (e) {
      setRetryError(e instanceof Error ? e.message : 'Retry failed');
      setRetrying(false);
    }
  };

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
            {retryError && (
              <div className="mt-4">
                <ErrorAlert message={retryError} />
              </div>
            )}
            <div className="mt-6">
              <SyncJobDetails
                job={adaptedJob}
                errors={errors}
                onRetry={canRetry ? handleRetry : undefined}
                retrying={retrying}
              />
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
