'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

export default function LogsPage() {
  const [files, setFiles] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const res = await fetch('/api/display');
        if (!res.ok) throw new Error(`Server returned ${res.status}`);
        const data = await res.json();

        // Handle both array and { logs: [...] } shapes
        const list: string[] = Array.isArray(data) ? data : (data.logs ?? data.files ?? []);
        setFiles(list);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load log list.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center gap-4">
        <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">
          ← Dashboard
        </Link>
        <h1 className="text-lg font-semibold text-gray-800">Ingested Log Files</h1>
      </header>

      <main className="flex-1 px-6 py-8 max-w-2xl mx-auto w-full">
        {loading && (
          <p className="text-sm text-gray-500 text-center mt-12">Loading…</p>
        )}

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {!loading && !error && files.length === 0 && (
          <div className="text-center mt-12 space-y-2">
            <p className="text-gray-500 text-sm">No log files found.</p>
            <Link href="/upload" className="text-blue-600 text-sm hover:underline">
              Upload the first log file →
            </Link>
          </div>
        )}

        {!loading && files.length > 0 && (
          <ul className="space-y-2">
            {files.map((name) => (
              <li key={name}>
                <Link
                  href={`/logs/${encodeURIComponent(name)}`}
                  className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm hover:border-blue-400 hover:bg-blue-50 transition-all group"
                >
                  <span className="text-gray-400 group-hover:text-blue-500 text-base">📄</span>
                  <span className="text-gray-700 group-hover:text-blue-700 font-medium">{name}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
