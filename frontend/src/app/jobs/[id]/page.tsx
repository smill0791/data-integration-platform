'use client';

import { useParams } from 'next/navigation';
import Link from 'next/link';
import { useSyncJob } from '@/hooks/useSyncJob';
import { useSyncErrors } from '@/hooks/useSyncErrors';
import SyncJobDetails from '@/components/SyncJobDetails';
import LoadingSpinner from '@/components/LoadingSpinner';
import ErrorAlert from '@/components/ErrorAlert';

export default function JobDetailPage() {
  const params = useParams();
  const jobId = Number(params.id);
  const { data: job, isLoading: jobLoading, error: jobError } = useSyncJob(jobId);
  const { data: errors, isLoading: errorsLoading } = useSyncErrors(jobId);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Link href="/" className="text-sm text-blue-600 hover:underline">
          &larr; Back to Dashboard
        </Link>

        {jobError && (
          <div className="mt-4">
            <ErrorAlert message={jobError.message} />
          </div>
        )}

        {jobLoading || errorsLoading ? (
          <LoadingSpinner />
        ) : job ? (
          <>
            <h1 className="mt-4 text-2xl font-bold text-gray-900">
              Sync Job #{job.id} — {job.sourceName}
            </h1>
            <div className="mt-6">
              <SyncJobDetails job={job} errors={errors || []} />
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
