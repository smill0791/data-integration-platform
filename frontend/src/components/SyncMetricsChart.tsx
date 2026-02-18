'use client';

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { DailyStatPoint } from '@/types/syncJob';

export default function SyncMetricsChart({ data }: { data: DailyStatPoint[] }) {
  if (data.length === 0) {
    return (
      <div className="rounded-lg bg-white p-8 text-center text-gray-500 shadow">
        No sync data available for chart.
      </div>
    );
  }

  return (
    <div className="rounded-lg bg-white p-6 shadow">
      <h3 className="mb-4 text-lg font-medium text-gray-900">Sync Activity</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis allowDecimals={false} />
          <Tooltip />
          <Legend />
          <Bar dataKey="syncsCompleted" name="Completed" fill="#22c55e" />
          <Bar dataKey="syncsFailed" name="Failed" fill="#ef4444" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
