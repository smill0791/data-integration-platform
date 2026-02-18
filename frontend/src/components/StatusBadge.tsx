'use client';

const statusStyles: Record<string, string> = {
  QUEUED: 'bg-gray-100 text-gray-800 animate-pulse',
  RUNNING: 'bg-blue-100 text-blue-800 animate-pulse',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
};

export default function StatusBadge({ status }: { status: string }) {
  const style = statusStyles[status] || 'bg-gray-100 text-gray-800';

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${style}`}>
      {status}
    </span>
  );
}
