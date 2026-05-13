'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';

export default function DashboardPage() {
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    await fetch('/api/logout', { method: 'POST' });
    router.push('/');
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-semibold text-gray-800">Security Log Viewer</h1>
        <button
          onClick={handleLogout}
          disabled={loggingOut}
          className="text-sm text-gray-500 hover:text-red-600 disabled:opacity-50 transition-colors"
        >
          {loggingOut ? 'Signing out…' : 'Sign out'}
        </button>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center px-4 gap-8">
        <p className="text-gray-500 text-sm">Choose an action</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 w-full max-w-lg">
          <Link
            href="/upload"
            className="group flex flex-col items-center justify-center gap-3 rounded-2xl bg-white border border-gray-200 shadow-sm hover:shadow-md hover:border-blue-400 transition-all px-6 py-10"
          >
            <span className="text-4xl">📤</span>
            <span className="text-base font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">
              Upload Logs
            </span>
            <span className="text-xs text-gray-400 text-center">
              Submit a Zscaler web proxy log file for ingestion
            </span>
          </Link>

          <Link
            href="/logs"
            className="group flex flex-col items-center justify-center gap-3 rounded-2xl bg-white border border-gray-200 shadow-sm hover:shadow-md hover:border-blue-400 transition-all px-6 py-10"
          >
            <span className="text-4xl">📋</span>
            <span className="text-base font-semibold text-gray-800 group-hover:text-blue-600 transition-colors">
              View Logs
            </span>
            <span className="text-xs text-gray-400 text-center">
              Browse and inspect ingested log files
            </span>
          </Link>
        </div>
      </main>
    </div>
  );
}
