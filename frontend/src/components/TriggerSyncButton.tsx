'use client';

import { useTriggerSync } from '@/hooks/useTriggerSync';

export default function TriggerSyncButton() {
  const { mutate, isPending, error } = useTriggerSync();

  return (
    <div>
      <button
        onClick={() => mutate()}
        disabled={isPending}
        className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {isPending ? (
          <>
            <svg className="mr-2 h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Syncing...
          </>
        ) : (
          'Trigger CRM Sync'
        )}
      </button>
      {error && <p className="mt-2 text-sm text-red-600">{error.message}</p>}
    </div>
  );
}
